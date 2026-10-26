package com.estore.order;

public interface OrderEventPort {
    void orderPlaced(Order order);
}
