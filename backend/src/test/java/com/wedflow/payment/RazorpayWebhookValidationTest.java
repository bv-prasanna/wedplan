package com.wedflow.payment;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RazorpayWebhookValidationTest {
 @Test void signatureUsesExactRawBody(){String raw="{\"event\":\"refund.processed\"}";String sig=RazorpaySignature.hmac("webhook-secret",raw);assertTrue(RazorpaySignature.verify("webhook-secret",raw,sig));assertFalse(RazorpaySignature.verify("webhook-secret",raw+" ",sig));}
 @Test void incorrectSecretRejected(){String raw="{}";assertFalse(RazorpaySignature.verify("wrong",raw,RazorpaySignature.hmac("correct",raw)));}
 @Test void missingSignatureRejected(){assertFalse(RazorpaySignature.verify("secret","{}",null));}
 @Test void signatureCaseInsensitive(){String raw="{}";assertTrue(RazorpaySignature.verify("secret",raw,RazorpaySignature.hmac("secret",raw).toUpperCase()));}
}
