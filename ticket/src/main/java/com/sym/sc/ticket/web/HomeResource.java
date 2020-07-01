package com.sym.sc.ticket.web;

import com.sym.sc.service.dto.OrderDTO;
import com.sym.sc.ticket.dao.TicketRepository;
import com.sym.sc.ticket.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mavlarn on 2018/2/14.
 */
@RestController
@RequestMapping("/api/ticket")
public class HomeResource {

    @Autowired
    private TicketService ticketService;

    @GetMapping("")
    public String get() {
        return "Welcome to ticket api!";
    }

    @PostMapping("/lockTicket")
    public String lockTicket(@RequestBody OrderDTO orderDTO){
        //如果锁票操作中有多个sql执行如先查询，后更新的，此处应该考虑使用分布式锁
        int cnt = ticketService.lockTicket(orderDTO);
        String result = cnt == 1 ? "lock ticket success":"lock ticket failed";
        return result;
    }


}
