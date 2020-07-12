package com.sym.sc.ticket.service;

import com.sym.sc.service.dto.OrderDTO;
import com.sym.sc.ticket.dao.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Transactional
    @JmsListener(destination = "order:new",
                containerFactory = "msgFactory")
    public void lockTicketFromMq(OrderDTO dto){
        logger.info("got new order:{}",dto.toString());
        //尝试锁票
        int lockTicketCnt = ticketRepository.locketTicket(dto.getCustomerId(),dto.getTicketNum());
        //锁票成功
        if(lockTicketCnt == 1){
            dto.setStatus("TICKET_LOCKED");
            //锁票成功发送消息到队列，驱动order服务创建订单
            jmsTemplate.convertAndSend("order:locked",dto);
        }else{
            // TODO: 2020/6/30 锁票失败
            dto.setStatus("TICKET_LOCK_FAILED");
            //锁票失败，直接发送失败消息给订单队列，创建失败的订单
            jmsTemplate.convertAndSend("order:failed",dto);
        }
    }

    @Transactional
    @JmsListener(destination = "order:ticket_move",
            containerFactory = "msgFactory")
    public void moveTicket(OrderDTO dto){
        logger.info("got move ticket:{}",dto.toString());
        int moveTicketCnt = ticketRepository.moveTicket(dto.getCustomerId(),dto.getTicketNum());
        //消息已经处理过
        if(moveTicketCnt == 0){
            logger.warn("tciket has been moved:{}",dto.toString());
        }
        //状态标记为已出票，发送消息到订单完成消息队列
        dto.setStatus("TICKET_MOVED");
        jmsTemplate.convertAndSend("order:finish",dto);
    }

    /**
     * 余额不够，解锁票
     * @param dto
     */
    @Transactional
    @JmsListener(destination = "order:ticket_error",
            containerFactory = "msgFactory")
    public void unlockTicket(OrderDTO dto){
        logger.info("got unlock ticket:{}",dto.toString());
        //出票异常，解锁票
        int unlockTicketCnt = ticketRepository.unlockTicket(dto.getCustomerId(),dto.getTicketNum());
        //消息已经处理过
        if(unlockTicketCnt == 0){
            logger.warn("tciket has been unlocked:{}",dto.toString());
        }
        //同时，回退移票
        int moveBackCnt = ticketRepository.moveBackTicket(dto.getCustomerId(),dto.getTicketNum());
        if(moveBackCnt == 0){
            logger.info("ticket unmoved or already move back");
        }
        //状态标记为已解锁，发送消息到订单完成消息队列
        jmsTemplate.convertAndSend("order:failed",dto);
    }

    @Transactional
    public int lockTicket(OrderDTO dto){
        int lockTicketCnt = ticketRepository.locketTicket(dto.getCustomerId(),dto.getTicketNum());
        logger.info("lock ticket count:{}",lockTicketCnt);
        try {
            Thread.sleep(10*1000);//睡10s模拟保证并发的产生
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lockTicketCnt;
    }
}
