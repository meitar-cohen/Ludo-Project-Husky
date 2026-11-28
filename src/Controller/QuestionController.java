package Controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Model.Question;
import Model.QuestionBank;
import Model.QuestionEvent;
import Model.QuestionEventCreator;
import Model.QuestionRecord;
import Model.QuestionRepository;

/**
 * בקר מרכזי לשאלות, בהתאם לתרשים המחלקות.
 * משתמש במחלקות הניהול, האימות וה-Factory Method הקיימות במקום לשכפל אותן.
 * ניהול השאלות שומר גם את שדות המטא-דאטה הנוספים של קובץ ה-CSV.
 */
public final class QuestionController {

    private final AdminAuthController authentication;
    private final Path csvPath;
    private final QuestionBank gameBank;
    private QuestionManagementController management;
    private String loadError = "";

    /** לשימוש הממשק: המאגר נטען רק בעת צורך, כך שכניסת מנהל אינה תלויה ב-CSV. */
    public QuestionController() {
        this(QuestionRepository.getProjectCsvPath(), new AdminAuthController());
    }

    /** מאפשר שימוש במאגר סיסמאות זמני בבדיקות. */
    public QuestionController(AdminAuthController authentication) {
        this(QuestionRepository.getProjectCsvPath(), authentication);
    }

    /** מאפשר בדיקות CRUD על עותק CSV זמני. */
    public QuestionController(Path csvPath, AdminAuthController authentication) {
        if (csvPath == null || authentication == null) {
            throw new IllegalArgumentException("CSV path and authentication are required");
        }
        this.csvPath = csvPath;
        this.authentication = authentication;
        this.gameBank = null;
    }

    /** שימוש במחלקת הניהול הקיימת; חשוב לצורך בדיקות התצוגה הישנות. */
    public QuestionController(QuestionManagementController management) {
        this(management, new AdminAuthController());
    }

    public QuestionController(QuestionManagementController management,
                              AdminAuthController authentication) {
        if (management == null || authentication == null) {
            throw new IllegalArgumentException("Controllers are required");
        }
        this.management = management;
        this.authentication = authentication;
        this.csvPath = null;  // הנתיב נשמר בתוך מחלקת הניהול שסופקה.
        this.gameBank = null;
    }

    /** בקר המשחק משתמש באותו מאגר בזיכרון שבו הוא השתמש קודם לכן. */
    public QuestionController(QuestionBank gameBank) {
        if (gameBank == null) {
            throw new IllegalArgumentException("Question bank is required");
        }
        this.csvPath = QuestionRepository.getProjectCsvPath();
        this.authentication = new AdminAuthController();
        this.gameBank = gameBank;
    }

    private QuestionManagementController manager() {
        if (management == null) {
            try {
                management = new QuestionManagementController(csvPath);
                loadError = "";
            } catch (IOException exception) {
                loadError = exception.getMessage();
                throw new IllegalStateException("Cannot load questions: " + loadError, exception);
            }
        }
        return management;
    }

    /** הפעולה המופיעה בתרשים: טעינת רשימת שאלות. */
    public List<Question> loadQuestions() {
        // טעינה חוזרת מקובץ פעיל במקום להחזיר צילום ישן של המידע.
        if (csvPath != null) management = null;
        return getAllQuestions();
    }

    /** כל שינוי נשמר מיידית; פעולה זו מאפשרת גם שמירה מפורשת לפי התרשים. */
    public boolean saveQuestions() {
        try {
            manager().save();
            loadError = "";
            return true;
        } catch (IOException | IllegalStateException exception) {
            loadError = exception.getMessage();
            return false;
        }
    }

    /** בחירה אקראית לפי רמה, על בסיס מאגר המשחק או המאגר שנבחר לבדיקות. */
    public Question getQuestionForSquare(String difficulty) {
        QuestionBank bank = gameBank;
        if (bank == null) bank = new QuestionBank(getAllQuestions());
        return bank.drawQuestion(difficulty);
    }

    /** שומר על Factory Method הקיים ועל התנהגות המשחק. */
    public QuestionEvent createQuestionEvent(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            throw new IllegalArgumentException("Question difficulty is required");
        }
        QuestionBank bank = gameBank;
        if (bank == null) bank = new QuestionBank(getAllQuestions());
        QuestionEventCreator creator = switch (difficulty.trim().toLowerCase(Locale.ROOT)) {
            case "easy" -> new QuestionEventCreator.Easy(bank);
            case "medium" -> new QuestionEventCreator.Medium(bank);
            case "hard" -> new QuestionEventCreator.Hard(bank);
            default -> throw new IllegalArgumentException(
                    "Unsupported question difficulty: " + difficulty);
        };
        return creator.create();
    }

    public boolean isCorrectAnswer(Question question, int selectedIndex) {
        if (question == null) throw new IllegalArgumentException("Question required");
        return question.getCorrectIndex() == selectedIndex;
    }

    public int getMoveSteps(String difficulty) {
        if (difficulty == null) throw new IllegalArgumentException("Difficulty required");
        return switch (difficulty.trim().toLowerCase(Locale.ROOT)) {
            case "easy" -> 1;
            case "medium" -> 2;
            case "hard" -> 3;
            default -> throw new IllegalArgumentException("Invalid difficulty: " + difficulty);
        };
    }

    /** אימות נשאר אצל בקר האימות הקיים. */
    public boolean authenticateAdmin(char[] password) throws IOException {
        return authentication.authenticate(password);
    }

    public boolean requiresSetup() {
        return authentication.requiresSetup();
    }

    public void setupPassword(char[] password, char[] confirmation) throws IOException {
        authentication.setupPassword(password, confirmation);
    }

    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();
        for (QuestionRecord record : manager().getQuestions()) {
            questions.add(record.getQuestion());
        }
        return List.copyOf(questions);
    }

    public String addQuestion(String text, String[] options,
                              int correct, String difficulty) {
        try {
            int id = add(new Question(text, options, correct, difficulty), "", "", "");
            return "Question added: " + id;
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            return exception.getMessage();
        }
    }

    public String editQuestion(int id, String text, String[] options,
                               int correct, String difficulty) {
        try {
            QuestionRecord existing = findRecord(id);
            update(id, new Question(text, options, correct, difficulty),
                    existing.getArchitecturalPattern(), existing.getCoverage(), existing.getSource());
            return "Question updated: " + id;
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            return exception.getMessage();
        }
    }

    public boolean deleteQuestion(int id) {
        try {
            delete(id);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            loadError = exception.getMessage();
            return false;
        }
    }

    public String validateQuestion(String text, String[] options,
                                   int correct, String difficulty) {
        try {
            Question question = new Question(text, options, correct, difficulty);
            new QuestionRecord(1, question, "", "", "");
            return ""; // מחרוזת ריקה: קלט תקין.
        } catch (IllegalArgumentException exception) {
            return exception.getMessage();
        }
    }

    public boolean hasEnoughQuestions() {
        return manager().getQuestions().size() >= 30;
    }

    public String getLoadError() {
        return loadError;
    }

    /** פעולות מורחבות הדרושות לממשק: משמרות את כל עמודות ה-CSV. */
    public List<QuestionRecord> getQuestionRecords() {
        return manager().getQuestions();
    }

    public int add(Question question, String pattern, String coverage, String source)
            throws IOException {
        return manager().add(question, pattern, coverage, source);
    }

    public void update(int id, Question question, String pattern,
                       String coverage, String source) throws IOException {
        manager().update(id, question, pattern, coverage, source);
    }

    public void delete(int id) throws IOException {
        manager().delete(id);
    }

    private QuestionRecord findRecord(int id) {
        for (QuestionRecord record : manager().getQuestions()) {
            if (record.getId() == id) return record;
        }
        throw new IllegalArgumentException("Question ID not found: " + id);
    }
}
