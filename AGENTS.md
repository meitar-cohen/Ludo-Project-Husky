Read before every task. These rules are mandatory unless the user overrides them in the current conversation.

## Workflow
1. Inspect the relevant existing code first.
2. Course lectures, tutorials, the SRS, the SDP and the class diagram are not committed to the repo. First look for them in `resources\temp\` (local, git-ignored); if not there, the user provides them or they exist on the user's computer. Ask for them **only when the task depends on them** (adding or changing classes, relationships, design patterns, requirements or scope, or a design decision that must follow the course or the project documents). For small or local changes (bug fix, tweak inside a method, test, UI detail) do not ask. If needed and not provided, ask for the file or its location before planning. Never guess. **Whenever the user provides such a source, save a copy in `resources\temp\`** and reuse it in later tasks instead of asking again.
3. **Propose a short plan and wait for approval before changing any file.** The plan lists: goal, files to change or create (by Model / View / Controller / Tests), any new class, tests to add, and risks to game logic.
4. Implement small, surgical changes that match the approved plan.
5. Compile and run the new tests plus the related existing tests. Report honestly what was and was not run.

If a request is ambiguous or has several reasonable designs, ask instead of choosing silently.

## Architecture (strict MVC)
- **No direct View ↔ Model link.** `View` never imports or references `Model`, and `Model` never imports or references `View` or Swing. All communication goes through the Controller, passing primitives, Strings, Enums or simple data.
- **All logic is in the Controller** (rules, turn flow, validation, captures, questions, surprises, win conditions). If unsure where logic belongs, it belongs in the Controller.
- **Model = simple functions only** (fields, getters, setters, trivial state updates). No game rules or workflows.
- **View = presentation and input only.** It forwards user actions to the Controller and makes no game decisions.

## Code rules
- **No inner, nested, local or anonymous classes.** Every new type goes in its own top-level file. This applies to every class in the project, especially all `Controller` and `Model` classes: do not add or modify nested classes in them.
- **Create only classes that appear in the class diagram.** Any other class (helper, utility, interface, enum) needs the user's explicit approval first.
- **Screens and panels (View).** Follow the existing `...Screen` naming convention for a significant screen. A screen made of several significant panels keeps each panel in its own file. A small panel (under ~100 lines) does not need its own file, and often does not need to be a separate panel at all: build it as a method inside the screen or its parent panel. Never solve this with a nested class. Any new panel class still follows the class-diagram rule above.
- Follow the existing naming and style. Do not refactor unrelated code or add dependencies without asking.
- **Enums.** Place every enum in the `Enums` package, as its own top-level file, never inside a Model, View or Controller class. A new enum still follows the class-diagram rule above.
- **No unrequested code.** Do not add methods, fields, parameters or features that are unused (never called) or go beyond what was asked. Every added method must be called from the code or by a test.

## Tests
- Every new feature or behavior change comes with tests, written in the same change.
- Follow the existing test conventions and location in the project. Do not assume a fixed test path, because it may change. Look at where the current tests live.
- Cover the normal case, the important edge cases, and (for bug fixes) the original failure.
- Never weaken, delete or skip existing tests to make a change pass.

## Preserve game logic
Make sure the existing rules keep working: dice and extra turns, taking pieces out, movement and the home path, exact-roll entry and finish, captures and protection, question squares, surprise squares, skipped turns, no-legal-move handling, history and win conditions.
If a change would alter or break a rule, **stop and ask**.

## Before saying "done"
- [ ] The plan was approved before changes were made.
- [ ] Logic is in the Controller, the Model has only simple functions, and there are no `View` ↔ `Model` references.
- [ ] No nested classes were added, and no classes outside the diagram without approval.
- [ ] New tests were added and pass, and the related existing tests still pass.
- [ ] The final report lists changed files, tests run, and open questions.