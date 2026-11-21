package com.devstack.pos.service;

import com.devstack.pos.entity.OrderDetail;
import com.devstack.pos.entity.ReturnOrder;
import com.devstack.pos.entity.ReturnOrderItem;
import com.devstack.pos.repository.ReturnOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnOrderService {

    private final ReturnOrderRepository returnOrderRepository;
    private final ReturnOrderItemService returnOrderItemService;
    private final ProductDetailService productDetailService;
    private final OrderDetailService orderDetailService;
    private final CustomerService customerService;
    private final SystemSettingsService systemSettingsService;

    public ReturnOrder saveReturnOrder(ReturnOrder returnOrder) {
        return returnOrderRepository.save(returnOrder);
    }

    public ReturnOrder updateReturnOrder(ReturnOrder returnOrder) {
        return returnOrderRepository.save(returnOrder);
    }

    @Transactional(readOnly = true)
    public Optional<ReturnOrder> findById(Integer id) {
        return returnOrderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ReturnOrder> findByReturnId(String returnId) {
        return returnOrderRepository.findByReturnId(returnId);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findByOrderId(Integer orderId) {
        return returnOrderRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findByCustomerEmail(String customerEmail) {
        return returnOrderRepository.findByCustomerEmail(customerEmail);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findByStatus(String status) {
        return returnOrderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findAllReturnOrders() {
        return returnOrderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findByReturnDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.findByReturnDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> findByStatusAndReturnDateBetween(String status, LocalDateTime startDate,
            LocalDateTime endDate) {
        return returnOrderRepository.findByStatusAndReturnDateBetween(status, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ReturnOrder> searchReturnOrders(String searchText, Integer orderId, String status,
            LocalDateTime startDate, LocalDateTime endDate) {
        return returnOrderRepository.searchReturnOrders(searchText, orderId, status, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Long countByStatus(String status) {
        Long count = returnOrderRepository.countByStatus(status);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public Double getTotalRefundAmount() {
        Double total = returnOrderRepository.getTotalRefundAmount();
        return total != null ? total : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getTotalRefundAmountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Double total = returnOrderRepository.getTotalRefundAmountByDateRange(startDate, endDate);
        return total != null ? total : 0.0;
    }

    @Transactional(readOnly = true)
    public Long countReturnsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Long count = returnOrderRepository.countReturnsByDateRange(startDate, endDate);
        return count != null ? count : 0L;
    }

    @Transactional(readOnly = true)
    public Double getTotalRefundAmountByCustomer(String customerName) {
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equals(customerName)) {
            return 0.0;
        }
        Double total = returnOrderRepository.getTotalRefundAmountByCustomerName(customerName);
        return total != null ? total : 0.0;
    }

    @Transactional(readOnly = true)
    public Double getTotalRefundAmountByCustomerAndDateRange(String customerName, LocalDateTime startDate,
            LocalDateTime endDate) {
        if (customerName == null || customerName.trim().isEmpty() || "Guest".equals(customerName)) {
            return 0.0;
        }
        Double total = returnOrderRepository.getTotalRefundAmountByCustomerNameAndDateRange(customerName, startDate,
                endDate);
        return total != null ? total : 0.0;
    }

    public ReturnOrder rejectReturn(Integer id, String rejectedBy) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            returnOrder.setStatus("REJECTED");
            returnOrder.setProcessedBy(rejectedBy);
            return returnOrderRepository.save(returnOrder);
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }

    public ReturnOrder completeReturn(Integer id, String processedBy) {
        Optional<ReturnOrder> optionalReturn = returnOrderRepository.findById(id);
        if (optionalReturn.isPresent()) {
            ReturnOrder returnOrder = optionalReturn.get();
            Double refundAmountBeforeAdjustment = returnOrder.getRefundAmount();

            // Reverse Customer Loyalty Points earned on returned items
            try {
                OrderDetail orderDetail = orderDetailService.findOrderDetail(Long.valueOf(returnOrder.getOrderId()));
                if (orderDetail != null && refundAmountBeforeAdjustment != null && refundAmountBeforeAdjustment > 0) {
                    com.devstack.pos.entity.Customer customer = null;
                    if (orderDetail.getCustomerId() != null) {
                        customer = customerService.findCustomer(orderDetail.getCustomerId());
                    } else if (orderDetail.getCustomerName() != null && !orderDetail.getCustomerName().trim().isEmpty() && !"Guest".equalsIgnoreCase(orderDetail.getCustomerName().trim())) {
                        customer = customerService.findByContact(orderDetail.getCustomerName().trim());
                    }

                    if (customer != null) {
                        double rate = 100.0;
                        var settings = systemSettingsService.getSystemSettingsForBusiness(orderDetail.getBusinessId());
                        if (settings != null && settings.getLoyaltyPointsRate() != null && settings.getLoyaltyPointsRate() > 0) {
                            rate = settings.getLoyaltyPointsRate();
                        }
                        double pointsToReverse = refundAmountBeforeAdjustment / rate;
                        double currentPoints = customer.getLoyaltyPoints();
                        double newPoints = Math.max(0.0, currentPoints - pointsToReverse);
                        customer.setLoyaltyPoints(newPoints);
                        customerService.updateCustomer(customer);

                        String currentNotes = returnOrder.getNotes() != null ? returnOrder.getNotes() : "";
                        String loyaltyNote = String.format(" [Reversed %.2f loyalty points]", pointsToReverse);
                        returnOrder.setNotes(currentNotes + loyaltyNote);
                        System.out.println("Reversed " + pointsToReverse + " loyalty points for customer: " + customer.getName() + " on return #" + returnOrder.getReturnId());
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to reverse loyalty points for return " + id + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Handle Credit Order Adjustments (Pending Payments)
            try {
                // Find associated order
                OrderDetail orderDetail = orderDetailService.findOrderDetail(Long.valueOf(returnOrder.getOrderId()));

                // If the order is PENDING (Credit/Cheque), we adjust the debt instead of
                // recording a cash refund
                if (orderDetail != null && "PENDING".equals(orderDetail.getPaymentStatus())) {
                    Double refundAmount = returnOrder.getRefundAmount();

                    if (refundAmount != null && refundAmount > 0) {
                        // 1. Reduce the Original Order Total Cost (Debt)
                        // This ensures "Pending Payments" total is correct
                        Double currentTotal = orderDetail.getTotalCost();
                        Double newTotal = Math.max(0, currentTotal - refundAmount);
                        orderDetail.setTotalCost(newTotal);

                        // 2. Reduce Balance if tracked
                        if (orderDetail.getBalance() != null) {
                            orderDetail.setBalance(Math.max(0, orderDetail.getBalance() - refundAmount));
                        }

                        // Save the updated order
                        orderDetailService.saveOrderDetail(orderDetail);

                        // 3. Update Return Order Notes
                        String currentNotes = returnOrder.getNotes() != null ? returnOrder.getNotes() : "";
                        String adjustmentNote = String
                                .format(" [Credit Adjustment: %.2f deducted from Pending Balance]", refundAmount);
                        returnOrder.setNotes(currentNotes + adjustmentNote);

                        // 4. Set Refund Amount to 0.0
                        // This ensures it doesn't count as "Cash Out" in Revenue calculations
                        // Revenue = Sales(Paid) - Refunds.
                        // Since we reduced Sales(Paid) amount effectively, we shouldn't subtract Refund
                        // too.
                        returnOrder.setRefundAmount(0.0);

                        System.out.println("Applied credit adjustment for Order #" + orderDetail.getCode() +
                                ". Reduced debt by " + refundAmount);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing credit adjustment for return " + id + ": " + e.getMessage());
                e.printStackTrace();
                // Continue with completion even if adjustment fails (though ideally shouldn't
                // happen)
            }

            returnOrder.setStatus("COMPLETED");
            returnOrder.setCompletionDate(LocalDateTime.now());
            returnOrder.setProcessedBy(processedBy);

            // Save the return order first to ensure status is updated
            ReturnOrder savedReturn = returnOrderRepository.save(returnOrder);
            System.out.println(
                    "Return order " + savedReturn.getReturnId() + " marked as COMPLETED and saved to database");

            // Restore inventory for all items in this return order
            try {
                restoreInventoryForReturnOrder(id);
                System.out.println("Inventory restored successfully for return order " + id);
            } catch (Exception e) {
                System.err
                        .println("Warning: Failed to restore inventory for return order " + id + ": " + e.getMessage());
                e.printStackTrace();
            }

            return savedReturn;
        }
        throw new RuntimeException("Return order not found with id: " + id);
    }

    /**
     * Restore inventory for all items in a return order
     */
    public void restoreInventoryForReturnOrder(Integer returnOrderId) {
        List<ReturnOrderItem> returnItems = returnOrderItemService.findByReturnOrderId(returnOrderId);

        for (ReturnOrderItem item : returnItems) {
            if (!item.getInventoryRestored() && item.getBatchCode() != null) {
                try {
                    // Restore stock to the original batch
                    productDetailService.restoreStock(item.getBatchCode(), item.getReturnQuantity());

                    // Mark as restored
                    item.setInventoryRestored(true);
                    returnOrderItemService.updateReturnOrderItem(item);
                } catch (Exception e) {
                    System.err.println("Failed to restore inventory for item: " + item.getId() +
                            ", batch: " + item.getBatchCode() + ", error: " + e.getMessage());
                    // Continue with other items even if one fails
                }
            }
        }
    }

    /**
     * Process a return order with item-level details
     * Creates the return order and saves all individual return items
     */
    public ReturnOrder processReturnWithItems(ReturnOrder returnOrder, List<ReturnOrderItem> returnItems) {
        try {
            // Save the return order first
            ReturnOrder savedReturn = saveReturnOrder(returnOrder);
            System.out.println(
                    "Return order saved with ID: " + savedReturn.getId() + ", Return ID: " + savedReturn.getReturnId());

            // Save all return items
            for (ReturnOrderItem item : returnItems) {
                item.setReturnOrderId(savedReturn.getId());
            }
            List<ReturnOrderItem> savedItems = returnOrderItemService.saveAllReturnOrderItems(returnItems);
            System.out.println("Saved " + savedItems.size() + " return order items");

            // If status is COMPLETED, immediately restore inventory
            if ("COMPLETED".equals(savedReturn.getStatus())) {
                try {
                    restoreInventoryForReturnOrder(savedReturn.getId());
                } catch (Exception e) {
                    System.err.println(
                            "Warning: Failed to restore inventory during return processing: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the transaction - return order is already saved
                }
            }

            return savedReturn;
        } catch (Exception e) {
            System.err.println("Error processing return with items: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process return order: " + e.getMessage(), e);
        }
    }

    public void deleteReturnOrder(Integer id) {
        returnOrderRepository.deleteById(id);
    }
}
