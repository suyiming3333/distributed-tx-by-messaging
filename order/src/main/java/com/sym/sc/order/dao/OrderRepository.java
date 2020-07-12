package com.sym.sc.order.dao;

import com.sym.sc.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by mavlarn on 2018/1/20.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**根据用户id查询所有**/
    List<Order> findAllByCustomerId(Long customerId);

    List<Order> findAllByStatusAndCreateDateBefore(String status, ZonedDateTime checkTime);

    /**根据uuid查询order**/
    Order findOneByUuid(String uuid);

    Order findOne(Long id);
}
