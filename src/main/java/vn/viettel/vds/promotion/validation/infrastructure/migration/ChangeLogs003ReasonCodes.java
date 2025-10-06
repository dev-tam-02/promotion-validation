package vn.viettel.vds.promotion.validation.infrastructure.migration;

import com.promix.platform.core.util.IdGenerator;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "003-create-reason-codes", order = "003", author = "system")
public class ChangeLogs003ReasonCodes {

    @Execution
    public void createReasonCodes(MongoTemplate mongoTemplate) {
        List<Map<String, Object>> reasonCodes = new ArrayList<>();

        // Customer Segment Reason Codes
        Map<String, Object> reasonCode1 = new HashMap<>();
        reasonCode1.put("_id", IdGenerator.generateId());
        reasonCode1.put("code", "CUSTOMER_SEGMENT_VIP");
        reasonCode1.put("category", "CUSTOMER");
        reasonCode1.put("severity", "INFO");
        reasonCode1.put("message", "Customer is in VIP segment");
        reasonCode1.put("description", "Validates customer is in VIP segment for exclusive offers");
        reasonCodes.add(reasonCode1);

        Map<String, Object> reasonCode2 = new HashMap<>();
        reasonCode2.put("_id", IdGenerator.generateId());
        reasonCode2.put("code", "CUSTOMER_SEGMENT_NEW");
        reasonCode2.put("category", "CUSTOMER");
        reasonCode2.put("severity", "INFO");
        reasonCode2.put("message", "Customer is new");
        reasonCode2.put("description", "Validates customer is in NEW segment for welcome offers");
        reasonCodes.add(reasonCode2);

        Map<String, Object> reasonCode3 = new HashMap<>();
        reasonCode3.put("_id", IdGenerator.generateId());
        reasonCode3.put("code", "CUSTOMER_SEGMENT_ELIGIBLE");
        reasonCode3.put("category", "CUSTOMER");
        reasonCode3.put("severity", "INFO");
        reasonCode3.put("message", "Customer is eligible for promotion");
        reasonCode3.put("description", "Validates customer segment is eligible for the promotion");
        reasonCodes.add(reasonCode3);

        // Order Amount Reason Codes
        Map<String, Object> reasonCode4 = new HashMap<>();
        reasonCode4.put("_id", IdGenerator.generateId());
        reasonCode4.put("code", "ORDER_AMOUNT_MIN");
        reasonCode4.put("category", "ORDER");
        reasonCode4.put("severity", "INFO");
        reasonCode4.put("message", "Order meets minimum amount requirement");
        reasonCode4.put("description", "Validates order amount meets the minimum threshold");
        reasonCodes.add(reasonCode4);

        // Time Window Reason Codes
        Map<String, Object> reasonCode5 = new HashMap<>();
        reasonCode5.put("_id", IdGenerator.generateId());
        reasonCode5.put("code", "TIME_WINDOW_WEEKEND");
        reasonCode5.put("category", "TIME");
        reasonCode5.put("severity", "INFO");
        reasonCode5.put("message", "Current time is within weekend window");
        reasonCode5.put("description", "Validates current time falls within weekend promotion hours");
        reasonCodes.add(reasonCode5);

        Map<String, Object> reasonCode6 = new HashMap<>();
        reasonCode6.put("_id", IdGenerator.generateId());
        reasonCode6.put("code", "TIME_WINDOW_BUSINESS");
        reasonCode6.put("category", "TIME");
        reasonCode6.put("severity", "INFO");
        reasonCode6.put("message", "Current time is within business hours");
        reasonCode6.put("description", "Validates current time falls within business hours promotion window");
        reasonCodes.add(reasonCode6);

        for (Map<String, Object> reasonCode : reasonCodes) {
            mongoTemplate.save(reasonCode, "reason_codes");
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(Query.query(Criteria.where("code").in(
                "CUSTOMER_SEGMENT_VIP", "CUSTOMER_SEGMENT_NEW", "CUSTOMER_SEGMENT_ELIGIBLE",
                "ORDER_AMOUNT_MIN", "TIME_WINDOW_WEEKEND", "TIME_WINDOW_BUSINESS"
        )), "reason_codes");
    }
}