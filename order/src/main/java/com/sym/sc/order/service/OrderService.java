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
}
