package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BaseContext;
import com.example.common.R;
import com.example.dto.OrdersDto;
import com.example.entity.OrderDetail;
import com.example.entity.Orders;
import com.example.service.AddressBookService;
import com.example.service.OrderDetailService;
import com.example.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private AddressBookService addressBookService;

    @GetMapping("/page")
    public Page<OrdersDto> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        Page<OrdersDto> ordersDtoPage = new Page<OrdersDto>(page, pageSize);
        Page<Orders> ordersPage = new Page<Orders>(page, pageSize);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<Orders>();
        queryWrapper.orderByAsc(Orders::getOrderTime);
        ordersService.page(ordersPage, queryWrapper);
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        List<OrdersDto> ordersDtos = ordersPage.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<OrderDetail>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId, item.getId());
            List<OrderDetail> list = orderDetailService.list(orderDetailLambdaQueryWrapper);
            ordersDto.setOrderDetails(list);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(ordersDtos);

        return ordersDtoPage;
    }

    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize) {
        Page<OrdersDto> orderDetailPage = new Page<OrdersDto>(page, pageSize);
        // 查询除了records 以外的所有分页信息
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<Orders>();
        ordersLambdaQueryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        ordersLambdaQueryWrapper.orderByDesc(Orders::getOrderTime);
        Page<Orders> ordersPage = new Page<Orders>(page, pageSize);
        ordersService.page(ordersPage, ordersLambdaQueryWrapper);

        BeanUtils.copyProperties(ordersPage, orderDetailPage, "records");

        List<OrdersDto> collect = ordersPage.getRecords().stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<OrderDetail>();
            queryWrapper.eq(OrderDetail::getOrderId, item.getId());
            List<OrderDetail> list = orderDetailService.list(queryWrapper);
            ordersDto.setOrderDetails(list);
            return ordersDto;
        }).collect(Collectors.toList());
        orderDetailPage.setRecords(collect);


        return R.success(orderDetailPage);
    }

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        ordersService.submit(orders);
        return R.success("下单成功");
    }

}
