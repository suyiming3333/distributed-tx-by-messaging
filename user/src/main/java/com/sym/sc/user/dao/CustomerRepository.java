package com.sym.sc.user.dao;

import com.sym.sc.user.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Created by mavlarn on 2018/1/20.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Customer findOneByUsername(String username);

    @Modifying
    @Query("update tb_customer set deposit = deposit - ?2 where id = ?1")
    int pay4Order(Long customerId,int amount);
}
