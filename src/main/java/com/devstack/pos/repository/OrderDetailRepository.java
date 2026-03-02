package com.devstack.pos.repository;

import com.devstack.pos.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

       @Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId ORDER BY o.issuedDate DESC")
       List<OrderDetail> findByCustomerId(@Param("customerId") Long customerId);

       // Get total amount spent by a customer (only PAID orders)
       @Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID'")
       Double getTotalSpentByCustomerId(@Param("customerId") Long customerId);

       @Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID' AND o.issuedDate BETWEEN :startDate AND :endDate")
       Double getTotalSpentByCustomerIdAndDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query("SELECT o FROM OrderDetail o WHERE o.customerName LIKE %:search% ORDER BY o.issuedDate DESC")
       List<OrderDetail> findByCustomerNameContaining(@Param("search") String search);

       @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
       Double getTotalRevenue();

       @Query("SELECT SUM(o.totalCost + o.discount) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
       Double getTotalGrossRevenue();

       @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
       Double getAverageOrderValue();

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.paymentStatus = 'PAID'")
       Long countPaidOrders();

       @Query("SELECT COUNT(o) FROM OrderDetail o")
       Long getTotalOrderCount();

       @Query("SELECT SUM(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
       Double getRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT SUM(o.totalCost + o.discount) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
       Double getGrossRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT o.customerName, SUM(o.totalCost) as total FROM OrderDetail o WHERE o.customerName IS NOT NULL AND o.paymentStatus = 'PAID' GROUP BY o.customerName ORDER BY total DESC")
       List<Object[]> getTopCustomersByRevenue();

       @Query("SELECT o FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
       List<OrderDetail> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Sales by cashier/operator (COUNT includes all orders, SUM only PAID orders)
       @Query("SELECT o.operatorEmail, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o GROUP BY o.operatorEmail ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
       List<Object[]> getSalesByCashier();

       @Query("SELECT o.operatorEmail, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.operatorEmail ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
       List<Object[]> getSalesByCashierByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Top customers with order count (COUNT includes all orders, SUM only PAID
       // orders)
       @Query("SELECT o.customerName, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
       List<Object[]> getTopCustomersWithOrderCount();

       @Query("SELECT o.customerName, COUNT(o), SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.customerName IS NOT NULL GROUP BY o.customerName ORDER BY SUM(CASE WHEN o.paymentStatus = 'PAID' THEN o.totalCost ELSE 0 END) DESC")
       List<Object[]> getTopCustomersWithOrderCountByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Count orders by date range (all orders, including pending payments)
       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate")
       Long countOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
       Long countPaidOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Average order value by date range (only PAID orders)
       @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
       Double getAverageOrderValueByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Find all pending payments
       @Query("SELECT o FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' ORDER BY o.issuedDate ASC")
       List<OrderDetail> findPendingPayments();

       // Find pending payments by payment method
       @Query("SELECT o FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.paymentMethod = :paymentMethod ORDER BY o.issuedDate ASC")
       List<OrderDetail> findPendingPaymentsByMethod(@Param("paymentMethod") String paymentMethod);

       // Get pending payments total by customer (all time)
       @Query("SELECT o.customerName, COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.customerName IS NOT NULL GROUP BY o.customerName")
       List<Object[]> getPendingPaymentsByCustomer();

       // Get pending payments total by customer (with date range)
       @Query("SELECT o.customerName, COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.paymentStatus = 'PENDING' AND o.customerName IS NOT NULL AND o.issuedDate BETWEEN :startDate AND :endDate GROUP BY o.customerName")
       List<Object[]> getPendingPaymentsByCustomerByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       // Get pending payments total by customer ID
       @Query("SELECT COALESCE(SUM(o.totalCost), 0.0) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PENDING'")
       Double getPendingPaymentsTotalByCustomerId(@Param("customerId") Long customerId);

       // Customer Purchase History Analysis Queries
       @Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId ORDER BY o.issuedDate DESC")
       List<OrderDetail> getCustomerPurchaseHistory(@Param("customerId") Long customerId);

       @Query("SELECT o FROM OrderDetail o WHERE o.customerId = :customerId AND o.issuedDate BETWEEN :startDate AND :endDate ORDER BY o.issuedDate DESC")
       List<OrderDetail> getCustomerPurchaseHistoryByDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.customerId = :customerId")
       Long getCustomerTotalOrders(@Param("customerId") Long customerId);

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.customerId = :customerId AND o.issuedDate BETWEEN :startDate AND :endDate")
       Long getCustomerTotalOrdersByDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID'")
       Long countPaidOrdersByCustomerId(@Param("customerId") Long customerId);

       @Query("SELECT COUNT(o) FROM OrderDetail o WHERE o.customerId = :customerId AND o.issuedDate BETWEEN :startDate AND :endDate AND o.paymentStatus = 'PAID'")
       Long countPaidOrdersByCustomerIdAndDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query("SELECT MIN(o.issuedDate) FROM OrderDetail o WHERE o.customerId = :customerId")
       LocalDateTime getCustomerFirstPurchaseDate(@Param("customerId") Long customerId);

       @Query("SELECT MAX(o.issuedDate) FROM OrderDetail o WHERE o.customerId = :customerId")
       LocalDateTime getCustomerLastPurchaseDate(@Param("customerId") Long customerId);

       @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID'")
       Double getCustomerAverageOrderValue(@Param("customerId") Long customerId);

       @Query("SELECT AVG(o.totalCost) FROM OrderDetail o WHERE o.customerId = :customerId AND o.paymentStatus = 'PAID' AND o.issuedDate BETWEEN :startDate AND :endDate")
       Double getCustomerAverageOrderValueByDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       @Query(value = "SELECT DATE(o.issued_date) as purchaseDate, SUM(o.total_cost) as totalAmount " +
                     "FROM order_detail o " +
                     "WHERE o.customer_id = :customerId AND o.payment_status = 'PAID' " +
                     "GROUP BY DATE(o.issued_date) " +
                     "ORDER BY DATE(o.issued_date) DESC", nativeQuery = true)
       List<Object[]> getCustomerPurchaseTrendByDate(@Param("customerId") Long customerId);

       @Query(value = "SELECT DATE(o.issued_date) as purchaseDate, SUM(o.total_cost) as totalAmount " +
                     "FROM order_detail o " +
                     "WHERE o.customer_id = :customerId AND o.payment_status = 'PAID' AND o.issued_date BETWEEN :startDate AND :endDate "
                     +
                     "GROUP BY DATE(o.issued_date) " +
                     "ORDER BY DATE(o.issued_date) DESC", nativeQuery = true)
       List<Object[]> getCustomerPurchaseTrendByDateRange(@Param("customerId") Long customerId,
                     @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

       /**
        * Get detailed top customers report (with date range)
        * Returns: customerName, orders, totalRevenue, refunds, netRevenue,
        * pendingPayments
        */
       @Query(value = "SELECT o.customer_name, " +
                     "COUNT(o.code) as orders, " +
                     "SUM(CASE WHEN o.payment_status = 'PAID' THEN o.total_cost ELSE 0 END) as totalRevenue, " +
                     "COALESCE(r.refund_amount, 0.0) as refunds, " +
                     "(SUM(CASE WHEN o.payment_status = 'PAID' THEN o.total_cost ELSE 0 END) - COALESCE(r.refund_amount, 0.0)) as netRevenue, "
                     +
                     "COALESCE(p.pending_amount, 0.0) as pendingPayments " +
                     "FROM order_detail o " +
                     "LEFT JOIN ( " +
                     "    SELECT ro.customer_email, SUM(roi.refund_amount) as refund_amount " +
                     "    FROM return_order_item roi " +
                     "    JOIN return_order ro ON roi.return_order_id = ro.id " +
                     "    WHERE ro.return_date BETWEEN :startDate AND :endDate " +
                     "    GROUP BY ro.customer_email " +
                     ") r ON o.customer_name = r.customer_email " +
                     "LEFT JOIN ( " +
                     "    SELECT o2.customer_name, SUM(o2.total_cost) as pending_amount " +
                     "    FROM order_detail o2 " +
                     "    WHERE o2.payment_status = 'PENDING' AND o2.issued_date BETWEEN :startDate AND :endDate " +
                     "    GROUP BY o2.customer_name " +
                     ") p ON o.customer_name = p.customer_name " +
                     "WHERE o.issued_date BETWEEN :startDate AND :endDate AND o.customer_name IS NOT NULL " +
                     "GROUP BY o.customer_name, r.refund_amount, p.pending_amount " +
                     "ORDER BY netRevenue DESC", nativeQuery = true)
       List<Object[]> getTopCustomersDetailedByDateRange(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);
}
