package com.sym.sc.user.service;

import com.sym.sc.service.dto.OrderDTO;
import com.sym.sc.user.dao.CustomerRepository;
import com.sym.sc.user.dao.PayInfoRepository;
import com.sym.sc.user.domain.Customer;
import com.sym.sc.user.domain.PayInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PayInfoRepository payInfoRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Transactional
    @JmsListener(destination = "order:pay",
                containerFactory = "msgFactory")
    public void lockTicketFromMq(OrderDTO dto){
        logger.info("got new order:{} to pay",dto.toString());

        //查询订单是否被支付过
        PayInfo payInfo = payInfoRepository.findOneByOrderId(dto.getId());
        //获取用户的余额信息
        Customer customer = customerRepository.findOne(dto.getCustomerId());
        if(payInfo != null){
            logger.warn("this order has already been paid,{}",dto.toString());
            return;
        }else{
            if(customer.getDeposit()<dto.getAmount()){
                return;//
            }

            //余额足够，则支付，并保存支付记录
            payInfo = new PayInfo();
            payInfo.setAmount(dto.getAmount());
            payInfo.setOrderId(dto.getId());
            payInfo.setStatus("PAID");
            //保存支付记录
            payInfoRepository.save(payInfo);
//        customer.setDeposit(customer.getDeposit()-dto.getAmount());//会有并发的问题，多次购买购买，只扣一次钱
//        customerRepository.save(customer);
            customerRepository.pay4Order(dto.getCustomerId(),dto.getAmount());
        }
        //发送消息到交票队列
        dto.setStatus("PAID");
        jmsTemplate.convertAndSend("order:ticket_move",dto);
    }
}
