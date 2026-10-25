package com.freshmind.backend.recipe;

import com.freshmind.backend.session.SessionAuthenticationInterceptor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class RecipeController {

    private final RecommendationService recommendations;

    public RecipeController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @GetMapping("/recommendations")
    public List<RecommendationResponse> recommendations(
            @RequestAttribute(SessionAuthenticationInterceptor.USER_ID_ATTRIBUTE) UUID userId,
            @RequestParam(defaultValue = "8") @Min(1) @Max(12) int limit) {
        return recommendations.recommend(userId, limit);
    }
}

