package com.example.project_backend04.dto.response.Trainer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerMatchingResult {
    
    private boolean isMatch;
    private String matchReason;
    private String mismatchReason;
    private List<String> matchingSpecialties;
    private List<String> missingSpecialties;
    private double matchScore; // 0.0 to 1.0
    private String recommendation;
    
    public static TrainerMatchingResult match(List<String> matchingSpecialties, double score) {
        return TrainerMatchingResult.builder()
            .isMatch(true)
            .matchingSpecialties(matchingSpecialties)
            .matchScore(score)
            .matchReason("Trainer has required specialties for this service")
            .recommendation("This trainer is suitable for the service")
            .build();
    }
    
    public static TrainerMatchingResult noMatch(String reason, List<String> missingSpecialties) {
        return TrainerMatchingResult.builder()
            .isMatch(false)
            .mismatchReason(reason)
            .missingSpecialties(missingSpecialties)
            .matchScore(0.0)
            .recommendation("This trainer may not be the best fit for this service")
            .build();
    }
    
    public boolean isGoodMatch() {
        return isMatch && matchScore >= 0.7;
    }
    
    public boolean isExcellentMatch() {
        return isMatch && matchScore >= 0.9;
    }
    
    public String getMatchQuality() {
        if (!isMatch) return "No match";
        if (matchScore >= 0.9) return "Excellent match";
        if (matchScore >= 0.7) return "Good match";
        if (matchScore >= 0.5) return "Fair match";
        return "Poor match";
    }
}