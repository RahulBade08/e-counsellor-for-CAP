package com.ecounsellor.backend.student.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps group labels to exact DB course names.
 * Input "Computer Science" -> 24 exact DB names -> used in SQL IN query.
 * Input "Computer Science and Engineering" (exact name) -> passes through unchanged.
 * Works from any client: Postman, Android, web.
 */
public class BranchGroups {

    private static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("Computer Science", Arrays.asList(
                "Computer Engineering",
                "Computer Engineering (Regional Language)",
                "Computer Engineering (Software Engineering)",
                "Computer Science",
                "Computer Science and Business Systems",
                "Computer Science and Design",
                "Computer Science and Engineering",
                "Computer Science and Engineering (Artificial Intelligence and Data Science)",
                "Computer Science and Engineering (Artificial Intelligence)",
                "Computer Science and Engineering (Cyber Security)",
                "Computer Science and Engineering (Internet of Things and Cyber Security Including Block Chain",
                "Computer Science and Engineering (Internet of Things and Cyber Security Including Block Chain Technology)",
                "Computer Science and Engineering (IoT)",
                "Computer Science and Engineering(Artificial Intelligence and Machine Learning)",
                "Computer Science and Engineering(Cyber Security)",
                "Computer Science and Engineering(Data Science)",
                "Computer Science and Information Technology",
                "Computer Science and Technology",
                "Computer Technology",
                "Cyber Security",
                "Data Engineering",
                "Data Science",
                "Internet of Things (IoT)",
                "Industrial IoT"
        ));

        GROUPS.put("Information Technology & Data Science", Arrays.asList(
                "Information Technology"
        ));

        GROUPS.put("Electronics & Communication", Arrays.asList(
                "Electronics & Telecommunication Engineering",
                "Electronics and Biomedical Engineering",
                "Electronics and Communication (Advanced Communication Technology)",
                "Electronics and Communication Engineering",
                "ELECTRONICS AND COMMUNICATION ENGINEERING (BIO-MEDICAL ENGINEERING)",
                "Electronics and Communication(Advanced Communication Technology)",
                "Electronics and Computer Engineering",
                "Electronics and Computer Science",
                "Electronics and Telecommunication Engg",
                "Electronics Engineering",
                "Electronics Engineering ( VLSI Design and Technology)",
                "VLSI",
                "Electrical and Computer Engineering"
        ));

        GROUPS.put("Electrical Engineering", Arrays.asList(
                "Electrical and Electronics Engineering",
                "Electrical Engg[Electronics and Power]",
                "Electrical Engineering",
                "Electrical, Electronics and Power",
                "Instrumentation and Control Engineering",
                "Instrumentation Engineering"
        ));

        GROUPS.put("Mechanical Engineering", Arrays.asList(
                "Automobile Engineering",
                "Mechanical & Automation Engineering",
                "Mechanical and Automation Engineering",
                "Mechanical and Mechatronics Engineering (Additive Manufacturing)",
                "MECHANICAL AND RAIL ENGINEERING",
                "Mechanical Engineering",
                "Mechanical Engineering Automobile",
                "Mechanical Engineering[Sandwich]",
                "Mechatronics Engineering",
                "Manufacturing Science and Engineering",
                "Automation and Robotics",
                "Robotics and Artificial Intelligence",
                "Robotics and Automation",
                "Aeronautical Engineering"
        ));

        GROUPS.put("Civil Engineering", Arrays.asList(
                "Civil and Environmental Engineering",
                "Civil and infrastructure Engineering",
                "Civil Engineering",
                "Civil Engineering (Structural Engineering)",
                "Civil Engineering and Planning",
                "Civil Engineering with Computer Application",
                "Structural Engineering"
        ));

        GROUPS.put("Artificial Intelligence & ML", Arrays.asList(
                "Artificial Intelligence",
                "Artificial Intelligence (AI) and Data Science",
                "Artificial Intelligence and Data Science",
                "Artificial Intelligence and Machine Learning"
        ));

        GROUPS.put("Chemical & Petroleum Technology", Arrays.asList(
                "Chemical Engineering",
                "Dyestuff Technology",
                "Oil and Paints Technology",
                "Oil Fats and Waxes Technology",
                "Oil Technology",
                "Oil,Oleochemicals and Surfactants Technology",
                "Paints Technology",
                "Paper and Pulp Technology",
                "Petro Chemical Engineering",
                "Pharmaceutical and Fine Chemical Technology",
                "Pharmaceuticals Chemistry and Technology",
                "Plastic and Polymer Engineering",
                "Plastic Technology",
                "Polymer Engineering and Technology",
                "Surface Coating Technology",
                "Metallurgy and Material Technology",
                "Mining Engineering"
        ));

        GROUPS.put("Textile & Fibre Technology", Arrays.asList(
                "Fibres and Textile Processing Technology",
                "Man Made Textile Technology",
                "Technical Textiles",
                "Textile Chemistry",
                "Textile Engineering / Technology",
                "Textile Technology",
                "Fashion Technology",
                "Printing and Packing Technology"
        ));

        GROUPS.put("Food & Bio Technology", Arrays.asList(
                "Bio Medical Engineering",
                "Bio Technology",
                "Food Engineering",
                "Food Engineering and Technology",
                "Food Technology",
                "Food Technology And Management",
                "Agricultural Engineering"
        ));

        GROUPS.put("Production & Manufacturing", Arrays.asList(
                "Production Engineering",
                "Production Engineering[Sandwich]",
                "Safety and Fire Engineering",
                "Fire Engineering"
        ));

        GROUPS.put("Other Engineering", Arrays.asList(
                "Architectural Assistantship"
        ));
    }

    public static List<String> expand(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        for (String input : inputs) {
            List<String> names = GROUPS.get(input);
            if (names != null) {
                result.addAll(names);  // group label -> expand all
            } else {
                result.add(input);     // exact name -> pass through
            }
        }
        return new ArrayList<>(result);
    }

    public static Map<String, List<String>> getGroups() {
        return GROUPS;
    }
}