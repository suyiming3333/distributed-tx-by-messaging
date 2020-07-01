package com.sym.sc.service;

import com.sym.sc.service.dto.OrderDTO;

/**
 * Created by mavlarn on 2018/2/14.
 */
public interface IOrderService {

    void create(OrderDTO dto);
    OrderDTO getMyOrder(Long id);
}
