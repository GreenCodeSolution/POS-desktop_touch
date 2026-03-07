package com.devstack.pos.service;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.repository.OrderDetailRepository;
import com.devstack.pos.repository.ReturnOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final ReturnOrderRepository returnOrderRepository;

    public OrderDetail saveOrderDetail(OrderDetail orderDetail) {
        return orderDetailRepository.save(orderDetail);
    }

    public OrderDetail findOrderDetail(Long code) {
        return orderDetailRepository.findById(code).orElse(null);
    }

    public List<OrderDetail> findAllOrderDetails() {
        return orderDetailRepository.findAll();
    }

    public List<OrderDetail> findByCustomerName(String customerName) {
        return orderDetailRepository.findByCustomerNameContaining(customerName);
    }

    @Transactional(readOnly = true)
    public Double getTotalSpentByCustomerId(Long customerId) {
        if (customerId == null) {
            return 0.0;
        }
        Double total = orderDetailRepository.getTotalSpentByCustomerId(customerId);
        Double refunds = returnOrderRepository.getTotalRefundAmountByCustomerId(customerId);

        total = total != null ? total : 0.0;
        refunds = refunds != null ? refunds : 0.0;

        return total - refunds;
    }

    // Analytics methods
    @Transactional(readOnly = true)
    public Double getTotalRevenue() {
        Double revenue = orderDetailRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getTotalGrossRevenue() {
        Double revenue = orderDetailRepository.getTotalGrossRevenue();
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getAverageOrderValue() {
        Double totalRevenue = orderDetailRepository.getTotalRevenue();
        Double totalRefunds = returnOrderRepository.getTotalRefundAmount();

        totalRevenue = totalRevenue != null ? totalRevenue : 0.0;
        totalRefunds = totalRefunds != null ? totalRefunds : 0.0;

        Double netRevenue = totalRevenue - totalRefunds;

        // Use only PAID orders for the count to match revenue criteria
        Long paidOrderCount = orderDetailRepository.countPaidOrders();

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netRevenue / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public Long getTotalOrderCount() {
        return orderDetailRepository.getTotalOrderCount();
    }

    @Transactional(readOnly = true)
    public Double getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getGrossRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getGrossRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersByRevenue() {
        return orderDetailRepository.getTopCustomersByRevenue();
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.findOrdersByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashier() {
        return orderDetailRepository.getSalesByCashier();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSalesByCashierByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getSalesByCashierByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCount() {
        return orderDetailRepository.getTopCustomersWithOrderCount();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopCustomersWithOrderCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getTopCustomersWithOrderCountByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Long countOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = orderDetailRepository.countOrdersByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public Double getAverageOrderValueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderDetailRepository.getRevenueByDateRange(startDate, endDate);
        Double refunds = returnOrderRepository.getTotalRefundAmountByDateRange(startDate, endDate);

        revenue = revenue != null ? revenue : 0.0;
        refunds = refunds != null ? refunds : 0.0;

        Double netRevenue = revenue - refunds;
        Long paidOrderCount = orderDetailRepository.countPaidOrdersByDateRange(startDate, endDate);

        if (paidOrderCount == null || paidOrderCount == 0) {
            return 0.0;
        }

        return netRevenue / paidOrderCount;
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPayments() {
        return orderDetailRepository.findPendingPayments();
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> findPendingPaymentsByMethod(String paymentMethod) {
        return orderDetailRepository.findPendingPaymentsByMethod(paymentMethod);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomer() {
        return orderDetailRepository.getPendingPaymentsByCustomer();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getPendingPaymentsByCustomerByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderDetailRepository.getPendingPaymentsByCustomerByDateRange(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Double getPendingPaymentsTotalByCustomerId(Long customerId) {
        Double result = orderDetailRepository.getPendingPaymentsTotalByCustomerId(customerId);
        return result != null ? result : 0.0;
    }

    @Transactional
    public boolean completePayment(Long orderCode) {
        OrderDetail order = findOrderDetail(orderCode);
        if (order != null && "PENDING".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("PAID");
            orderDetailRepository.save(order);
            return true;
        }
        return false;
    }
}
