package com.samjay.email_service.utils;

import com.samjay.email_service.enumerations.OrderCreator;

public class AppExtensions {

    private AppExtensions() {
    }

    public static final String ORDER_APPROVAL_SUBJECT = "Order Approval Notification";

    public static String getVerificationMailBody(String verificationCode) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Verify Your Email</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">Verify Your Email Address</h2>
                
                        <p>Thank you for registering with us! To complete your registration, please verify your email address.</p>
                
                        <p>Use the verification code below to activate your account:</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 32px; letter-spacing: 5px;">
                """ + verificationCode + """
                            </h1>
                        </div>
                
                        <p>Enter this code on the verification page to activate your account.</p>
                
                        <p style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #ff9800; border-radius: 4px; margin: 20px 0;">
                            <strong>Important:</strong> This verification code will expire in <strong>15 minutes</strong>.
                        </p>
                
                        <p>If you did not request this verification code, please ignore this email or contact our support team.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """;
    }

    public static String getOrderCreationMailBody(OrderCreator orderCreator, String counterpartyUsername,
                                                  String orderReferenceNumber) {

        String counterpartyRole = orderCreator == OrderCreator.BUYER ? "buyer" : "seller";

        String actionMessage = switch (orderCreator) {
            case BUYER ->
                    "Please visit your mobile app dashboard to review and approve this order to proceed with your wallet deposit and complete the payment.";
            case SELLER ->
                    "Please visit your mobile app dashboard to review and approve this order so that the buyer can proceed with their payment.";
        };

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Order Creation Notification</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">Order Creation Notification</h2>
                
                        <p>A <strong>%s</strong> with username <strong>%s</strong> has created an escrow delivery order with the following reference number:</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 3px;">%s</h1>
                        </div>
                
                        <p style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #ff9800; border-radius: 4px; margin: 20px 0;">
                            <strong>Action Required:</strong> %s
                        </p>
                
                        <p>If you did not expect this notification, please ignore this email or contact our support team.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """.formatted(counterpartyRole, counterpartyUsername, orderReferenceNumber, actionMessage);
    }

    public static String getOrderApprovalBySellerMailBody(String buyerUsername, String orderReferenceNumber,
                                                          OrderCreator orderCreator) {

        String title = "Order Approval Confirmation";

        String bodyMessage = switch (orderCreator) {

            // Buyer created order → Seller approved → notify seller (creator)
            case BUYER ->
                    ("Thank you for approving this order. The buyer with username <strong>%s</strong> has been notified and will now proceed with their payment. "
                            + "We will notify you once the payment has been completed and the order is ready to be processed."
                    ).formatted(buyerUsername);

            // Seller created order → Buyer approved → notify seller (creator)
            case SELLER ->
                    ("The buyer with username <strong>%s</strong> has approved your order and has been notified to proceed with payment. "
                            + "We will notify you once the payment has been completed and the order is ready to be processed."
                    ).formatted(buyerUsername);
        };

        String actionBanner = "";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">%s</h2>
                
                        <p>Your escrow delivery order with reference number:</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 3px;">%s</h1>
                        </div>
                
                        <p>%s</p>
                
                        %s
                
                        <p>If you did not approve this order or have any concerns, please contact our support team immediately.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """.formatted(title, title, orderReferenceNumber, bodyMessage, actionBanner);
    }

    public static String getOrderApprovalByBuyerMailBody(String sellerUsername, String orderReferenceNumber, OrderCreator orderCreator) {

        String title = switch (orderCreator) {

            // Buyer created order → Seller approved → notify buyer to pay
            case BUYER -> "Order Approved — Payment Required";

            // Seller created order → Buyer approved → notify buyer (approver)
            case SELLER -> "Order Approval Confirmation";
        };

        String bodyMessage = switch (orderCreator) {

            // Buyer created order → Seller approved → buyer must pay
            case BUYER ->
                    ("The seller with username <strong>%s</strong> has approved your order. You may now proceed with your" +
                            " payment so that delivery processing can begin.")
                            .formatted(sellerUsername);

            // Seller created order → Buyer approved → confirm approval + instruct payment
            case SELLER ->
                    "Thank you for approving this order. Please proceed with your payment so that delivery processing can begin.";
        };

        String actionBanner = """
                <p style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #ff9800; border-radius: 4px; margin: 20px 0;">
                    <strong>Action Required:</strong> Please log in to your mobile app dashboard to make payment for this order so that it can be processed for delivery.
                </p>
                """;

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">%s</h2>
                
                        <p>Your escrow delivery order with reference number:</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 3px;">%s</h1>
                        </div>
                
                        <p>%s</p>
                
                        %s
                
                        <p>If you did not expect this notification or have any concerns, please contact our support team immediately.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """.formatted(title, title, orderReferenceNumber, bodyMessage, actionBanner);
    }

    public static String getPaymentInitiationMailBody(String orderReferenceNumber, String paymentUrl) {

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Complete Your Payment</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">Complete Your Payment</h2>
                
                        <p>Your escrow delivery order with reference number:</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 3px;">%s</h1>
                        </div>
                
                        <p>is ready for payment. Please click the button below to complete your payment and have your order processed for delivery.</p>
                
                        <p style="background-color: #fff3cd; padding: 15px; border-left: 4px solid #ff9800; border-radius: 4px; margin: 20px 0;">
                            <strong>Action Required:</strong> Payment must be completed promptly to avoid delays in processing your order.
                        </p>
                
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="background-color: #2196F3; color: white; padding: 14px 32px; text-decoration: none; border-radius: 4px; font-size: 16px; font-weight: bold; display: inline-block;">
                                Complete Payment
                            </a>
                        </div>
                
                        <p style="color: #888; font-size: 13px;">If the button above does not work, copy and paste the link below into your browser:</p>
                        <p style="color: #2196F3; font-size: 13px; word-break: break-all;">%s</p>
                
                        <p>If you did not request this payment or have any concerns, please contact our support team immediately.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """.formatted(orderReferenceNumber, paymentUrl, paymentUrl);
    }

    public static String getPaymentCompletionMailBody(String orderReferenceNumber, String amount, boolean isBuyer) {

        String title = isBuyer ? "Payment Successful" : "Buyer Payment Received";

        String bodyMessage = isBuyer
                ? "Your payment of ₦<strong>%s</strong> for the escrow delivery order has been received and confirmed.".formatted(amount)
                : "The buyer has successfully completed payment of ₦<strong>%s</strong> for the escrow delivery order.".formatted(amount);

        String subMessage = isBuyer
                ? "Your order is now being processed for delivery. We will notify you once your order is on its way."
                : "The order is now being processed for delivery. We will notify you once a driver has been assigned to pick up and deliver the item.";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                </head>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 5px;">
                        <h2 style="color: #2196F3;">%s</h2>
                
                        <p>%s</p>
                
                        <div style="background-color: #f0f8ff; padding: 20px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <h1 style="color: #2196F3; margin: 0; font-size: 24px; letter-spacing: 3px;">%s</h1>
                        </div>
                
                        <p>%s</p>
                
                        <p style="background-color: #e8f5e9; padding: 15px; border-left: 4px solid #4CAF50; border-radius: 4px; margin: 20px 0;">
                            <strong>Payment Confirmed:</strong> The funds are securely held in escrow and will be released upon successful delivery.
                        </p>
                
                        <p>If you have any questions or concerns regarding this payment, please contact our support team immediately.</p>
                
                        <p style="margin-top: 30px;">Best regards,<br>The Team</p>
                
                        <hr style="margin-top: 40px; border: none; border-top: 1px solid #ddd;">
                    </div>
                </body>
                </html>
                """.formatted(title, title, bodyMessage, orderReferenceNumber, subMessage);
    }
}