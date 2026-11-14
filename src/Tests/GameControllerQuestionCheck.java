package Tests;

import Controller.GameController;
import Controller.MovementController;
import Model.Game;
import Model.Player;

public class GameControllerQuestionCheck {

    public static void main(String[] args) {
        Player[] players = {
            new Player("A", "Red"),
            new Player("B", "Blue")
        };
        Game game = new Game(players, "Easy");
        GameController controller = new GameController(
                game, new MovementController(game));

        if (controller.getQuestion() == null) {
            throw new AssertionError("Expected a current question");
        }
        if (!controller.isCorrectQuestionAnswer(0)) {
            throw new AssertionError("Expected answer 0 to be correct");
        }
        if (controller.isCorrectQuestionAnswer(1)) {
            throw new AssertionError("Expected answer 1 to be incorrect");
        }
        if (controller.isCorrectQuestionAnswer(-1)
                || controller.isCorrectQuestionAnswer(4)) {
            throw new AssertionError("Out-of-range answers must be incorrect");
        }

        System.out.println("All game controller question checks passed.");
    }
}
