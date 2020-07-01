package com.sym.sc.ticket.dao;

import com.sym.sc.ticket.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by mavlarn on 2018/1/20.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByOwner(Long owner);

    Ticket findOneByTicketNum(Long ticketNum);

    @Override
    @Modifying(clearAutomatically = true)//清除缓存
    Ticket save(Ticket ticket);

    /**
     * 通过mysql customerId is null 实现幂等性，保证锁表只能够一个人
     * @param customerId
     * @param ticketNum
     * @return
     */
    @Modifying
    @Query("update tb_ticket set lock_user = ?1 where lock_user is null and ticket_num = ?2")
    int locketTicket(Long customerId,Long ticketNum);

    /**
     * 用户支付完订单后，出票给用户：ownerId设置为用户，lockUser设置为空
     * @param customerId
     * @param ticketNum
     * @return
     */
    @Modifying
    @Query("update tb_ticket set owner = ?1, lock_user = null where lock_user = ?1 and ticket_num = ?2")
    int moveTicket(Long customerId,Long ticketNum);
}
