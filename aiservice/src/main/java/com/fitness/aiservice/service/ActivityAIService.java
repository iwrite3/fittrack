package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    private final OpenRouterService aiClient;
    public Recommendation generateRecommendation(Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = aiClient.getAnswer(prompt);
        log.info("RESPONSE FROM AI : {}" , aiResponse);

        return processAiResponse(activity , aiResponse);
    }
private Recommendation processAiResponse(Activity activity , String aiResponse){
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);
            JsonNode textNode = rootNode.path("choices")
                    .get(0).path("message").path("content");

        String raw = textNode.asText();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        String jsonContent = (start >= 0 && end > start) ? raw.substring(start, end + 1) : raw.trim();
        log.info("PARSE RESPONSE FROM AI: {}",jsonContent);
        JsonNode analysisJson = mapper.readTree(jsonContent);
        JsonNode analysisNode = analysisJson.path("analysis");
        StringBuilder fullAnalysis = new StringBuilder();
        addAnalysisSection(fullAnalysis , analysisNode , "overall" , "Overall: ");
            addAnalysisSection(fullAnalysis , analysisNode , "intensity" , "Intensity: ");
            addAnalysisSection(fullAnalysis , analysisNode , "calories" , "Calories: ");
            addAnalysisSection(fullAnalysis , analysisNode , "forYourProfile" , "For your profile: ");
            Integer caloriesBurned = readCalories(analysisJson);
            if (caloriesBurned == null || caloriesBurned <= 0) {
                log.warn("AI gave no usable calories, using fallback estimate");
                caloriesBurned = fallbackCalories(activity);
            }

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));
return Recommendation.builder()
        .activityId(activity.getId())
        .userId(activity.getUserId())
        .activityType(activity.getType())
        .caloriesBurned(caloriesBurned)
        .recommendation(fullAnalysis.toString().trim())
        .improvements(improvements)
        .suggestions(suggestions)
        .safety(safety)
        .createdAt(LocalDateTime.now())
        .build();
        }
        catch(Exception e){
            e.printStackTrace();
            return  createDefaultRecommendation(activity);
        }

}

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .caloriesBurned(fallbackCalories(activity))
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your work"))
                .suggestions(Collections.singletonList("Consult a physician"))
                .safety(Arrays.asList("Always warm up before exercise",   "Stay hydrated" ,
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety= new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(item -> safety.add(item.asText()));

        }
        return safety.isEmpty() ? Collections.singletonList("Follow general safety guidelines") : safety;

    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if(suggestionsNode.isArray()){
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.has("description") ? suggestion.path("description").asText() : "No description provided";
                suggestions.add(String.format("%s: %s", workout , description ));
            });

        }
        return suggestions.isEmpty() ? Collections.singletonList("No specific suggestions provided") : suggestions;

    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();

        if(improvementsNode.isArray())
        {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.has("recommendation") ? improvement.path("recommendation").asText()  : "No description provided";
                improvements.add(String.format("%s: %s", area , detail));
            });
        }
        return improvements.isEmpty() ? Collections.singletonList("No specific improvements provided") : improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode())
        {
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }    }

    private String createPromptForActivity(Activity activity) {
        String bmi = "not available";
        if (activity.getWeight() != null && activity.getHeight() != null && activity.getHeight() > 0) {
            double meters = activity.getHeight() / 100.0;
            bmi = String.format("%.1f", activity.getWeight() / (meters * meters));
        }
        return String.format("""
        You are a friendly, safety-conscious fitness coach.
        A user logged the workout below. The activity name was typed freely by the user,
        so interpret it sensibly. Personalize the advice to the user's profile.
        If a profile value is "not provided", do not guess it.

        Workout:
        - Activity: %s
        - Duration: %s minutes
        - Extra details: %s

        User profile:
        - Age: %s years
        - Gender: %s
        - Weight: %s kg
        - Height: %s cm
        - BMI: %s

        Estimate the calories burned: pick the typical MET value for this activity, then
        calories = MET x weight in kg x duration in hours. If the weight is "not provided", assume 70 kg.

        Return the response strictly in JSON format, with no text outside the JSON.
        "caloriesBurned" is required: your estimate as a single whole number of kcal (digits only, greater than 0).
        {
            "caloriesBurned" : <whole number>,
            "analysis" : {
                "overall" : "short overall assessment of this workout",
                "intensity" : "how demanding this activity and duration are for this person",
                "calories" : "one sentence on how you estimated the calories (MET, weight, duration)",
                "forYourProfile" : "how this workout suits the user's age, gender, weight and height"
            },
            "improvements" : [{
                "area" : "Area name",
                "recommendation" : "Detailed recommendation"
            }],
            "suggestions" : [{
                "workout" : "Workout Name",
                "description" : "Detailed workout description"
            }],
            "safety" : ["Safety point1", "Safety point2"]
        }
        """,
                activity.getType(),
                activity.getDuration(),
                activity.getAdditionalMetrics(),
                orNotProvided(activity.getAge()),
                orNotProvided(activity.getGender()),
                orNotProvided(activity.getWeight()),
                orNotProvided(activity.getHeight()),
                bmi);
    }

    // Looks for the calories value under the names the AI might use, at the top level or inside "analysis".
    private Integer readCalories(JsonNode json) {
        String[] keys = {"caloriesBurned", "estimatedCaloriesBurned", "calories_burned", "totalCaloriesBurned"};
        for (String key : keys) {
            for (JsonNode parent : new JsonNode[]{json, json.path("analysis")}) {
                JsonNode node = parent.path(key);
                if (node.isNumber()) return node.asInt();
                if (node.isTextual()) {
                    Integer value = parseCalories(node.asText());
                    if (value != null) return value;
                }
            }
        }
        return null;
    }

    // Rough estimate (MET 5 = moderate exercise) used only if the AI gives no number.
    private Integer fallbackCalories(Activity activity) {
        double weight = activity.getWeight() != null ? activity.getWeight() : 70.0;
        double hours = (activity.getDuration() != null ? activity.getDuration() : 0) / 60.0;
        return (int) Math.round(5.0 * weight * hours);
    }

    // Accepts "350" or "about 350 kcal"; returns null if there is no number.
    private Integer parseCalories(String text) {
        if (text == null) return null;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 5) return null;
        return Integer.valueOf(digits);
    }

    private String orNotProvided(Object value) {
        return value == null ? "not provided" : value.toString();
    }
}
