package com.wedflow.api;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class ApiValidationTest {
 private final Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
 @Test void rejectsInvalidEmail(){assertFalse(validator.validate(new AuthController.Credentials("bad","abcdefghijkl")).isEmpty());}
 @Test void rejectsShortPassword(){assertFalse(validator.validate(new AuthController.Credentials("a@example.com","short")).isEmpty());}
 @Test void acceptsValidCredentials(){assertTrue(validator.validate(new AuthController.Credentials("a@example.com","longpassword12")).isEmpty());}
 @Test void rejectsZeroWeddingBudget(){assertFalse(validator.validate(new WeddingController.WeddingInput("Wedding","Bengaluru",LocalDate.now(),0)).isEmpty());}
 @Test void rejectsMissingWeddingTitle(){assertFalse(validator.validate(new WeddingController.WeddingInput("","Bengaluru",LocalDate.now(),100)).isEmpty());}
 @Test void rejectsNegativeBudgetCategory(){assertFalse(validator.validate(new PlanningController.BudgetInput("Catering",-1)).isEmpty());}
 @Test void rejectsBlankTask(){assertFalse(validator.validate(new PlanningController.TaskInput("",LocalDate.now())).isEmpty());}
 @Test void rejectsZeroGuestCount(){assertFalse(validator.validate(new CateringController.MenuInput(null,"Dinner",0)).isEmpty());}
 @Test void rejectsUnknownDiet(){assertFalse(validator.validate(new CateringController.ItemInput("Rice","UNKNOWN",10)).isEmpty());}
 @Test void acceptsJainDiet(){assertTrue(validator.validate(new CateringController.ItemInput("Rice","JAIN",10)).isEmpty());}
 @Test void rejectsInvalidReview(){assertFalse(validator.validate(new OperationsController.ReviewInput(6,"bad")).isEmpty());}
 @Test void rejectsNegativePayment(){assertFalse(validator.validate(new PaymentsController.PaymentIntent(UUID.randomUUID(),-1)).isEmpty());}
}
