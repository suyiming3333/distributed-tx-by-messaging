package com.sym.sc.order.service;

import com.sym.sc.order.dao.OrderRepository;
import com.sym.sc.order.domain.Order;
import com.sym.sc.service.dto.OrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Transactional
    @JmsListener(destination = "order:locked",
                containerFactory = "msgFactory")
    public void handleNewOrder(OrderDTO dto){
        logger.info("start to create order:{}",dto.toString());
        //根据uuid查询订单，是否存在
        Order order = orderRepository.findOneByUuid(dto.getUuid());
        //如果不为空，证明该消息已经处理过
        if(order!=null){
            logger.warn("msg already processed");
        }else{
            //创建订单
            Order createOrder = createOrder(dto);
            createOrder = orderRepository.save(createOrder);
            dto.setId(createOrder.getId());
        }
        //订单创建完，发送到待支付队列，到custom服务消费
        dto.setStatus("NEW");
        jmsTemplate.convertAndSend("order:pay",dto);
    }

    @Transactional
    @JmsListener(destination = "order:finish",
            containerFactory = "msgFactory")
    public void finishOrder(OrderDTO dto){
        logger.info("start to finish order:{}",dto.toString());
        //根据id查询订单
        Order order = orderRepository.findOne(dto.getId());
        order.setStatus("FNINISH");
        orderRepository.save(order);
    }

    /**
     * 处理失败的订单
     * 1、一开始就锁票失败了，直接新建一个失败订单
     * 2、锁票成功，但扣费失败，需要解锁票，然后订单更新为失败
     * 3、定时任务定时检查超时未完成的订单，设置为订单失败
     * @param dto
     */
    @Transactional
    @JmsListener(destination = "order:failed",
            containerFactory = "msgFactory")
    public void handleFailedOrder(OrderDTO dto){
        logger.info("start to handle failed order:{}",dto.toString());
        Order order = new Order();
        //订单id为空，说明是锁票失败了
        if(dto.getId() == null){
            order.setCustomerId(dto.getCustomerId());
            order.setTickerNum(dto.getTicketNum());
            order.setReason("ticket lock failed");
        }else{
            //订单id不为空，则通过订单id查出订单
            order = orderRepository.findOne(dto.getId());
            //由于余额不够导致订单失败
            if("DEPOSIT_NOT_ENOUGHT".equals(dto.getStatus())){
                order.setStatus("DEPOSIT_NOT_ENOUGHT");
            }
            if("TIMEOUT".equals(dto.getStatus())){
                order.setStatus("TIMEOUT");
            }
        }
        //创建失败订单
        order.setStatus("FAILED");
        orderRepository.save(order);
    }

    private Order createOrder(OrderDTO dto) {
        Order order = new Order();
        order.setUuid(dto.getUuid());
        order.setAmount(dto.getAmount());
        order.setTitle(dto.getTitle());
        order.setCustomerId(dto.getCustomerId());
        order.setTickerNum(dto.getTicketNum());
        order.setStatus("NEW");
        order.setCreateDate(ZonedDateTime.now());
        return order;
    }

    public void checkTimeOrders(){
        ZonedDateTime checkTime = ZonedDateTime.now().minusMinutes(1L);
        //查出超时的订单进行失败处理
        List<Order> orderList = orderRepository.findAllByStatusAndCreateDateBefore("NEW",checkTime);
        orderList.forEach(order ->{
            logger.error("time out order:{}",order.toString());
            OrderDTO dto = new OrderDTO();
            dto.setId(order.getId());
            dto.setTicketNum(order.getTickerNum());
            dto.setUuid(order.getUuid());
            dto.setAmount(order.getAmount());
            dto.setTitle(order.getTitle());
            dto.setCustomerId(order.getCustomerId());
            dto.setStatus("TIMEOUT");
            jmsTemplate.convertAndSend("order:failed",dto);
        });
    }
}
