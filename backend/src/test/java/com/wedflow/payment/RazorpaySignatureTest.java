package com.wedflow.payment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RazorpaySignatureTest {
 @Test void knownHmacVector(){assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",RazorpaySignature.hmac("key","The quick brown fox jumps over the lazy dog"));}
 @Test void validCheckoutSignature(){String s=RazorpaySignature.hmac("secret","order_1|pay_1");assertTrue(RazorpaySignature.checkout("secret","order_1","pay_1",s));}
 @Test void wrongPaymentRejected(){String s=RazorpaySignature.hmac("secret","order_1|pay_1");assertFalse(RazorpaySignature.checkout("secret","order_1","pay_2",s));}
 @Test void tamperedBodyRejected(){String s=RazorpaySignature.hmac("secret","body");assertFalse(RazorpaySignature.verify("secret","changed",s));}
 @Test void emptySecretRejected(){assertFalse(RazorpaySignature.verify("","body","a".repeat(64)));}
 @Test void malformedSignatureRejected(){assertFalse(RazorpaySignature.verify("secret","body","bad"));}
}
