package com.sym.sc.order.web;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.GeneratorImplBase;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.sym.sc.service.IOrderService;
import com.sym.sc.service.dto.OrderDTO;
import com.sym.sc.order.dao.OrderRepository;
import com.sym.sc.order.domain.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Created by mavlarn on 2018/1/20.
 */
@RestController
@RequestMapping("/api/order")
public class OrderResource implements IOrderService {

//    @PostConstruct
//    public void init() {
//        Order order = new Order();
//        order.setAmount(100);
//        order.setTitle("MyOrder");
//        orderRepository.save(order);
//    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    private TimeBasedGenerator timeBasedGenerator = Generators.timeBasedGenerator();

    /**
     * 选择ticket,创建订单，发送给mq
     * @param dto
     */
    @PostMapping("")
    public void create(@RequestBody OrderDTO dto) {
        //设置uuid用于标记订单创建订单的消息是否已经被处理过
        dto.setUuid(timeBasedGenerator.generate().toString());
        //创建订单消息发送给ticket服务监听的消息队列
        jmsTemplate.convertAndSend("order:new",dto);
    }

    @GetMapping("/{id}")
    public OrderDTO getMyOrder(@PathVariable Long id) {
        Order order = orderRepository.findOne(id);
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setAmount(order.getAmount());
        dto.setTitle(order.getTitle());
        return dto;
    }

    @GetMapping("")
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

}
