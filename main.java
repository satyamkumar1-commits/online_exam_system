import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Online Examination System
 * Concepts: ArrayList, Stack, Queue, Sorting, Methods and File Handling.
 *
 * Compile: javac Main.java
 * Run:     java Main
 *
 * Keep exam.txt in the same folder as Main.java.
 */
public class Main {
    private static final String QUESTION_FILE = "exam.txt";
    private static final String RESULTS_FILE = "results.txt";

    private static final Scanner scanner = new Scanner(System.in);
    private static final ArrayList<CandidateResult> results = new ArrayList<>();

    // Stack stores answered questions for review (latest answer is reviewed first).
    private static final Stack<AnsweredQuestion> reviewStack = new Stack<>();

    private static String lastCandidate = "";

    static class Question {
        int id;
        String text;
        String optionA, optionB, optionC, optionD;
        char correctAnswer;

        Question(int id, String text, String a, String b, String c, String d,
                 char correctAnswer) {
            this.id = id;
            this.text = text;
            this.optionA = a;
            this.optionB = b;
            this.optionC = c;
            this.optionD = d;
            this.correctAnswer = Character.toUpperCase(correctAnswer);
        }

        void display() {
            System.out.println("\nQ" + id + ". " + text);
            System.out.println("A. " + optionA);
            System.out.println("B. " + optionB);
            System.out.println("C. " + optionC);
            System.out.println("D. " + optionD);
        }
    }

    static class AnsweredQuestion {
        Question question;
        char givenAnswer;
        boolean correct;

        AnsweredQuestion(Question question, char givenAnswer, boolean correct) {
            this.question = question;
            this.givenAnswer = givenAnswer;
            this.correct = correct;
        }
    }

    static class CandidateResult {
        String name;
        int correct;
        int wrong;
        int marks;
        int totalQuestions;

        CandidateResult(String name, int correct, int wrong, int marks,
                        int totalQuestions) {
            this.name = name;
            this.correct = correct;
            this.wrong = wrong;
            this.marks = marks;
            this.totalQuestions = totalQuestions;
        }
    }

    public static void main(String[] args) {
        loadResults();

        System.out.println("======================================");
        System.out.println("       ONLINE EXAMINATION SYSTEM");
        System.out.println("======================================");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Start Exam");
            System.out.println("2. Review Last Exam");
            System.out.println("3. View Rank List");
            System.out.println("4. Exit");
            System.out.print("Enter menu option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    startExam();
                    break;
                case "2":
                    reviewLastExam();
                    break;
                case "3":
                    showRankList();
                    break;
                case "4":
                    saveResults();
                    System.out.println("Results saved. Thank you!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid menu option. Enter 1, 2, 3 or 4.");
            }
        }
    }

    // Reads question records from exam.txt.
    // Format: id|question|optionA|optionB|optionC|optionD|correctAnswer
    private static ArrayList<Question> loadQuestions() {
        ArrayList<Question> questions = new ArrayList<>();

        Path path = Paths.get(QUESTION_FILE);
        if (!Files.exists(path)) {
            System.out.println("Error: " + QUESTION_FILE + " was not found.");
            System.out.println("Keep exam.txt in the same folder where you run the program.");
            return questions;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);
                if (parts.length != 7) {
                    System.out.println("Skipping invalid record at line " + lineNumber);
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0].trim());
                    char key = parts[6].trim().toUpperCase().charAt(0);

                    if ("ABCD".indexOf(key) == -1) {
                        System.out.println("Skipping invalid answer key at line " + lineNumber);
                        continue;
                    }

                    questions.add(new Question(
                        id, parts[1].trim(), parts[2].trim(), parts[3].trim(),
                        parts[4].trim(), parts[5].trim(), key
                    ));
                } catch (Exception e) {
                    System.out.println("Skipping invalid record at line " + lineNumber);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + QUESTION_FILE + ": " + e.getMessage());
        }

        return questions;
    }

    private static void startExam() {
        ArrayList<Question> questions = loadQuestions();

        if (questions.isEmpty()) {
            System.out.println("No valid questions available. Exam cannot start.");
            return;
        }

        System.out.print("Enter candidate name: ");
        String candidateName = scanner.nextLine().trim();

        if (candidateName.isEmpty()) {
            System.out.println("Candidate name cannot be empty.");
            return;
        }

        // Queue ensures skipped questions are placed at the end.
        Queue<Question> questionQueue = new ArrayDeque<>(questions);
        reviewStack.clear();

        int correct = 0;
        int wrong = 0;
        int marks = 0;

        System.out.println("\nExam started for " + candidateName + ".");
        System.out.println("Enter A, B, C, D to answer or S to skip.");

        while (!questionQueue.isEmpty()) {
            Question current = questionQueue.poll();
            current.display();

            char answer = readAnswer();

            if (answer == 'S') {
                questionQueue.offer(current);
                System.out.println("Q" + current.id + " skipped and moved to the end of the queue.");
                continue;
            }

            boolean isCorrect = (answer == current.correctAnswer);
            reviewStack.push(new AnsweredQuestion(current, answer, isCorrect));

            if (isCorrect) {
                correct++;
                marks += 4;
                System.out.println("Correct answer! +4 marks.");
            } else {
                wrong++;
                marks -= 1;
                System.out.println("Wrong answer. Correct answer: " +
                                   current.correctAnswer + " (-1 mark).");
            }
        }

        CandidateResult result =
            new CandidateResult(candidateName, correct, wrong, marks, questions.size());

        results.add(result);
        lastCandidate = candidateName;
        saveResults();

        System.out.println("\nExam submitted. " + candidateName +
                           " | Correct: " + correct +
                           " | Wrong: " + wrong);
        System.out.println("Marks: " + marks + "/" + (questions.size() * 4));
        System.out.println("Your result has been saved in " + RESULTS_FILE + ".");
    }

    private static char readAnswer() {
        while (true) {
            System.out.print("Your answer (A/B/C/D/S): ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.length() == 1 && "ABCDS".contains(input)) {
                return input.charAt(0);
            }

            System.out.println("Invalid option. Enter A, B, C, D or S (skip).");
        }
    }

    // Displays the Stack from newest answer to oldest without removing its contents.
    private static void reviewLastExam() {
        if (reviewStack.isEmpty()) {
            System.out.println("No answered questions are available for review.");
            return;
        }

        System.out.println("\n===== REVIEW: " + lastCandidate + " =====");
        for (int i = reviewStack.size() - 1; i >= 0; i--) {
            AnsweredQuestion item = reviewStack.get(i);
            System.out.println("\nQ" + item.question.id + ". " + item.question.text);
            System.out.println("Your answer: " + item.givenAnswer);
            System.out.println("Correct answer: " + item.question.correctAnswer);
            System.out.println("Result: " + (item.correct ? "Correct" : "Wrong"));
        }
    }

    // Sorts candidates by marks (descending), then correct answers (descending).
    private static void showRankList() {
        if (results.isEmpty()) {
            System.out.println("No examination results available yet.");
            return;
        }

        ArrayList<CandidateResult> sorted = new ArrayList<>(results);
        sorted.sort((a, b) -> {
            int byMarks = Integer.compare(b.marks, a.marks);
            if (byMarks != 0) return byMarks;
            return Integer.compare(b.correct, a.correct);
        });

        System.out.println("\n========== RANK LIST ==========");
        for (int i = 0; i < sorted.size(); i++) {
            CandidateResult r = sorted.get(i);
            System.out.println((i + 1) + ". " + r.name + " | " + r.marks +
                               " marks | Correct: " + r.correct +
                               " | Wrong: " + r.wrong);
        }
    }

    // Loads previously saved results when the application starts.
    private static void loadResults() {
        Path path = Paths.get(RESULTS_FILE);
        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                if (p.length != 5) continue;

                try {
                    results.add(new CandidateResult(
                        p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), Integer.parseInt(p[4])
                    ));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed result rows.
                }
            }
        } catch (IOException e) {
            System.out.println("Could not load previous results: " + e.getMessage());
        }
    }

    // Saves all results. The file is rewritten to avoid duplicate appends on each save.
    private static void saveResults() {
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(RESULTS_FILE),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            for (CandidateResult r : results) {
                writer.write(r.name.replace("|", " ") + "|" + r.correct + "|" +
                             r.wrong + "|" + r.marks + "|" + r.totalQuestions);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Could not save results: " + e.getMessage());
        }
    }
}
