package Controller;

/**
 * Observer design pattern - the observer side. The subject is {@link GameController}: a view
 * (or anything else) registers with {@link GameController#addCaptureListener(CaptureListener)}
 * and is told every time a piece is captured. The controller does not know what the listeners
 * do with the news (the board shows a kick animation), so the rules stay in the Controller and
 * the pictures stay in the View.
 */
public interface CaptureListener {

    /** Called once for every capture, after the captured piece went back to its castle. */
    void onCapture(CaptureEvent event);
}
