package com.marigoldgames.rockpaperscissors;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.marigoldgames.rockpaperscissors.ai.SelectionStrategy;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final Stats stats = new Stats();
    private final Map<Trainer.EventType, Long> messageLastSeen = new EnumMap<Trainer.EventType, Long>(Trainer.EventType.class);
    private Menu menu;
    private Trainer model;
    private String dataFileName;
    private boolean trainerEnabled = true;
    private long maxVerbosity;
    private Toast toast;
    private boolean reportReady = false;
    private int docReportCount;
    private int docReportCountMax;
    private int trainerHintsCount;
    private MediaPlayer loseSound;
    private MediaPlayer drawSound;
    private MediaPlayer winSound;
    private MediaPlayer readySound;
    private MediaPlayer reportSound;
    private MediaPlayer clickSound;
    private MediaPlayer achievementSound;
    private MediaPlayer hintSound;
    private MediaPlayer warningSound;

    private static void fadeInThenOut(final View intermittent, final long in, final long out) {
        intermittent.setAlpha(0f);
        intermittent.setVisibility(View.VISIBLE);
        intermittent.animate().alpha(1f).setDuration(in).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                intermittent.animate().alpha(0f).setDuration(out).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        intermittent.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private static int toPercent(final int num, final int den) {
        if (den == 0) {
            return 0;
        } else {
            return (num * 100) / den;
        }
    }

    private static Long zeroIfNull(final Long val) {
        if (val == null) {
            return 0L;
        } else {
            return val;
        }
    }

    private void showAbout() {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setIcon(R.drawable.tick)
                .setTitle("Rock (Paper, Scissors) Doctor")
                .setView(getLayoutInflater().inflate(R.layout.about, null, false))
                .setNeutralButton("Credits", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLicense();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        d.setCanceledOnTouchOutside(true);
        d.show();
    }

    private void showLicense() {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setIcon(R.drawable.tick)
                .setTitle("Credits")
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(getLayoutInflater().inflate(R.layout.attribution, null, false)).create();

        d.setCanceledOnTouchOutside(true);
        d.show();
    }

    private void showClearData() {
        new AlertDialog.Builder(this)
                .setMessage("This will delete all accumulated data from previous sessions and start a new session. Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        model.resetAll();
                        stats.updateDisplay();
                        updateProgress();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private CharSequence[] getSessionStatsTable() {
        return new CharSequence[]{
                "Games Played " + model.getEventsSession(),
                "Wins " + model.getWinSession() + " (" + toPercent(model.getWinSession(), model.getEventsSession()) + "%)",
                "Draws " + model.getDrawSession() + " (" + toPercent(model.getDrawSession(), model.getEventsSession()) + "%)",
                "Losses " + model.getLoseSession() + " (" + toPercent(model.getLoseSession(), model.getEventsSession()) + "%)",
                "Win Streak " + model.getWinStreakSession(),
                "Draw Streak " + model.getDrawStreakSession(),
                "Lose Streak " + model.getLoseStreakSession(),};
    }

    private CharSequence[] getAllTimeStatsTable() {
        return new CharSequence[]{
                "Games Played " + model.getEventsAllTime(),
                "Wins " + model.getWinAllTime() + " (" + toPercent(model.getWinAllTime(), model.getEventsAllTime()) + "%)",
                "Draws " + model.getDrawAllTime() + " (" + toPercent(model.getDrawAllTime(), model.getEventsAllTime()) + "%)",
                "Losses " + model.getLoseAllTime() + " (" + toPercent(model.getLoseAllTime(), model.getEventsAllTime()) + "%)",
                "Win Streak " + model.getWinStreakAllTime(),
                "Draw Streak " + model.getDrawStreakAllTime(),
                "Lose Streak " + model.getLoseStreakAllTime(),};
    }

    private void showStats(final CharSequence[] thisStats, final CharSequence[] nextStats, final String thisText, final String nextText) {
        new AlertDialog.Builder(this).setTitle("Statistics")
                .setIcon(R.drawable.question)
                .setNeutralButton(nextText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStats(nextStats, thisStats, nextText, thisText);
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setItems(thisStats, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void showStats() {
        showStats(getSessionStatsTable(), getAllTimeStatsTable(), "This Session", "All Time");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        final SharedPreferences sharedPref = getSharedPreferences("preferences.dat", MODE_PRIVATE);
        final SelectionStrategy selectionStrategy = SelectionStrategy.valueOf(sharedPref.getString("SelectionStrategy", SelectionStrategy.MOST_CONFIDENT_PICKS.toString()));
        final int maxHistoryDepth = sharedPref.getInt("MaxHistoryDepth", 3);
        final double predictorConfidence = sharedPref.getFloat("Confidence", .9f);
        final int countingHistoryDepth = sharedPref.getInt("CountingHistoryDepth", 128);
        final int metaHistoryDepth = sharedPref.getInt("MetaHistoryDepth", 2);
        final double metaConfidence = sharedPref.getFloat("MetaConfidence", .9f);
        maxVerbosity = sharedPref.getLong("RepeatAdviceInterval", 4000L);
        dataFileName = sharedPref.getString("DataFileName", "state.dat");
        docReportCount = sharedPref.getInt("EventsUntilDocReportReady", 32);
        docReportCountMax = docReportCount;
        trainerHintsCount = sharedPref.getInt("EventsUntilTrainerHintsStart", 20);

        model = new Trainer(selectionStrategy, maxHistoryDepth, predictorConfidence, countingHistoryDepth, metaHistoryDepth, metaConfidence);

        try {
            final FileInputStream fis = openFileInput(dataFileName);
            model.read(fis);
            fis.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.move_button_base).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onMove(v, event);
            }
        });

        model.setEvent(new Trainer.Event() {
            @Override
            public void apply(String eventText, Trainer.EventType idx) {
                if (trainerEnabled) {
                    if (trainerHintsCount > 0) {
                        --trainerHintsCount;
                        return;
                    }
                    final long now = Calendar.getInstance().getTimeInMillis();
                    final Long lastSeen = zeroIfNull(messageLastSeen.get(idx));
                    if (now - lastSeen > maxVerbosity) {
                        switch (idx) {
                            case WARNING:
                                warningSound.start();
                                break;
                            case ACHIEVEMENT:
                                achievementSound.start();
                                break;
                            default:
                            case HINT:
                                hintSound.start();
                                break;
                        }

                        toToast(eventText);
                        messageLastSeen.put(idx, now);
                    }
                }
            }
        });

        stats.updateDisplay();
        updateProgress();

        loseSound = MediaPlayer.create(this, R.raw.lose);
        drawSound = MediaPlayer.create(this, R.raw.draw);
        winSound = MediaPlayer.create(this, R.raw.win);
        readySound = MediaPlayer.create(this, R.raw.ready);
        reportSound = MediaPlayer.create(this, R.raw.report);
        clickSound = MediaPlayer.create(this, R.raw.click);
        achievementSound = MediaPlayer.create(this, R.raw.achievement);
        hintSound = MediaPlayer.create(this, R.raw.hint);
        warningSound = MediaPlayer.create(this, R.raw.warning);
    }

    @Override
    protected void onDestroy() {
        try {
            final FileOutputStream fos = openFileOutput(dataFileName, MODE_PRIVATE);
            model.write(fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        loseSound.release();
        drawSound.release();
        winSound.release();
        readySound.release();
        reportSound.release();
        clickSound.release();
        achievementSound.release();

        super.onDestroy();
    }

    private void onMove(final View view, final Move move) {

        switch (model.onMove(move)) {
            case WIN:
                winSound.start();
                switch (move) {
                    case ROCK:
                        fadeInThenOut(findViewById(R.id.move_button_flare_rock), 100, 50);
                        break;
                    case PAPER:
                        fadeInThenOut(findViewById(R.id.move_button_flare_paper), 100, 50);
                        break;
                    case SCISSORS:
                        fadeInThenOut(findViewById(R.id.move_button_flare_scissors), 100, 50);
                        break;
                    default:
                        break;
                }
                break;
            case LOSE:
                loseSound.start();
                fadeInThenOut(findViewById(R.id.move_button_warp), 100, 100);
                findViewById(R.id.move_button_base).startAnimation(AnimationUtils.loadAnimation(this, R.anim.horiz_shake));
                break;
            case DRAW:
            default:
                drawSound.start();
                findViewById(R.id.move_button_base).startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                break;
        }

        stats.updateDisplay();
        updateProgress();
        if (docReportCount > 0) {
            --docReportCount;
            if (docReportCount == 0) {
                reportReady = true;
                readySound.start();
                if (docReportCount > (Integer.MAX_VALUE / 2)) {
                    docReportCount = Integer.MAX_VALUE;
                } else {
                    docReportCountMax *= 2;
                }
                docReportCount = docReportCountMax;
                toToast("Your Doctor's Report is ready.");
            }
        }
    }

    private void updateProgress() {
        final int ratioSession = (int) ((model.ratioSession() + 1d) * 50d);
        final int ratioAllTime = (int) ((model.ratioAllTime() + 1d) * 50d);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setProgress(Math.min(ratioSession, ratioAllTime));
        progressBar.setSecondaryProgress(Math.max(ratioSession, ratioAllTime));
    }

    public void onScissors(final View view) {
        onMove(view, Move.SCISSORS);
    }

    public void onPaper(final View view) {
        onMove(view, Move.PAPER);
    }

    public void onRock(final View view) {
        onMove(view, Move.ROCK);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        if (id == R.id.menu_about) {
            showAbout();
            return true;
        } else if (id == R.id.menu_stats) {
            reportSound.start();
            showStats();
            return true;
        } else if (id == R.id.menu_new_session) {
            model.resetSession();
            stats.updateDisplay();
            updateProgress();
            return true;
        } else if (id == R.id.menu_clear_data) {
            showClearData();
            return true;
        } else if (id == R.id.menu_doc) {
            if (!reportReady) {
                toToast("The Doctor's Report is not yet ready. Play a few more games.");
                return true;
            } else {
                reportSound.start();
                showDoc();
                return true;
            }
        } else if (id == R.id.menu_disable_hints) {
            item.setVisible(false);
            menu.findItem(R.id.menu_enable_hints).setVisible(true);
            trainerEnabled = false;
            return true;
        } else if (id == R.id.menu_enable_hints) {
            item.setVisible(false);
            menu.findItem(R.id.menu_disable_hints).setVisible(true);
            trainerEnabled = true;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onStats(View view) {
        clickSound.start();
        stats.incrementMode();
    }

    public boolean onMove(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            final double th = (7f / 12f + Math.atan2(event.getY() / v.getHeight() - .5f, event.getX() / v.getWidth() - .5f) / (2f * Math.PI)) % 1f;
            if (th < 1f / 3f) {
                onRock(v);
            } else if (th < 2f / 3f) {
                onPaper(v);
            } else {
                onScissors(v);
            }
        }
        return true;
    }

    private void toToast(final String text) {
        final LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.stats_toast,
                (ViewGroup) findViewById(R.id.stats_toast_root));

        ((TextView) layout.findViewById(R.id.stats_toast_text)).setText(text);

        if (toast != null) {
            toast.cancel();
        }

        toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    private void showDoc() {
        final String responseAdvice;
        final String patternAdvice;
        final String strategyAdvice;
        final String moveAdvice;
        final String scoreAdvice;

        final AbstractMap.SimpleImmutableEntry<Move, Move> response = model.getFavouriteResponse();
        int count0 = model.getResponseCount(response);
        int count1 = model.getResponseCount(new AbstractMap.SimpleImmutableEntry<Move, Move>(response.getKey(), Move.betterThan(response.getValue())));
        int count2 = model.getResponseCount(new AbstractMap.SimpleImmutableEntry<Move, Move>(response.getKey(), Move.worseThan(response.getValue())));
        responseAdvice = "Your most predictable counter is to play " + response.getValue().toString() + " after your opponent plays " + response.getKey().toString() + ".\n\nYou do this " + toPercent(count0, count0 + count1 + count2) + "% of the chances you get.\n\nTry to get this down to around 33% to be less predictable.";

        final AbstractMap.SimpleImmutableEntry<Move, Move> tell = model.getFavouriteTell();
        int count4 = model.getTellCount(tell);
        int count5 = model.getTellCount(new AbstractMap.SimpleImmutableEntry<Move, Move>(tell.getKey(), Move.betterThan(tell.getValue())));
        int count6 = model.getTellCount(new AbstractMap.SimpleImmutableEntry<Move, Move>(tell.getKey(), Move.worseThan(tell.getValue())));
        patternAdvice = "Your most predictable combo is to play " + tell.getValue().toString() + " after you've played " + tell.getKey().toString() + ".\n\nYou do this " + toPercent(count4, count4 + count5 + count6) + "% of the chances you get.\n\nTry to get this down to around 33% to be less predictable.";

        switch (model.getFavouriteModel()) {
            case "Meta":
                strategyAdvice = "Your strategy is really good.\n\nYou often second-guessed your opponent's moves.";
                break;
            case "Predictor":
                strategyAdvice = "Your opponent's most successful strategy was to remember your move sequence.\n\nTry not to repeat the same patterns.";
                break;
            case "Counter":
                strategyAdvice = "You have a bias towards one move over all others and this makes you predictable.\n\nTry to play each move with similar frequency.";
                break;
            default:
            case "Random":
                strategyAdvice = "Your strategy is about average.\n\nAn opponent who could play randomly would aim for a draw.\n\nYou should try to second-guess your opponents moves to improve your score.";
                break;
        }

        int max = Math.max(model.getPaperSession(), Math.max(model.getRockSession(), model.getScissorsSession()));
        String move;
        if (model.getPaperSession() == max) {
            move = "PAPER";
        } else if (model.getRockSession() == max) {
            move = "ROCK";
        } else {
            move = "SCISSORS";
        }
        moveAdvice = "Your favourite move is " + move + ".\n\nYou do this " + toPercent(max, model.getEventsSession()) + "% of the time.\n\nTry to get this down to around 33% to be less predictable.";

        final int ratioSession = (int) ((model.ratioSession() + 1d) * 50d);
        final int ratioAllTime = (int) ((model.ratioAllTime() + 1d) * 50d);

        scoreAdvice = "Your score for this session was " + ratioSession + "%.\n\nA score of 50% or more means you win more often than you lose.\n\n"
                + "Your all time score is " + ratioAllTime + "%.";

        final Intent intent = new Intent(this, DocReport.class);
        intent.putExtra("responseAdvice", responseAdvice);
        intent.putExtra("patternAdvice", patternAdvice);
        intent.putExtra("strategyAdvice", strategyAdvice);
        intent.putExtra("moveAdvice", moveAdvice);
        intent.putExtra("scoreAdvice", scoreAdvice);
        startActivity(intent);
    }

    private enum Mode {
        WIN_DRAW_LOSE_SESSION("Win/Lose/Draw - this session"),
        WIN_DRAW_LOSE_ALL_TIME("Win/Lose/Draw - all time"),
        ROCK_PAPER_SCISSORS_SESSION("Rock/Paper/Scissors - this session"),
        ROCK_PAPER_SCISSORS_ALL_TIME("Rock/Paper/Scissors - all time"),
        WIN_DRAW_LOSE_STREAK_SESSION("Longest Win/Lose/Draw streak - this session"),
        WIN_DRAW_LOSE_STREAK_ALL_TIME("Longest Win/Lose/Draw streak - all time"),
        ROCK_PAPER_SCISSORS_STREAK_SESSION("Longest Rock/Paper/Scissors streak - this session"),
        ROCK_PAPER_SCISSORS_STREAK_ALL_TIME("Longest Rock/Paper/Scissors streak - all time");

        final String description;

        Mode(final String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private class Stats {
        Mode currentMode = Mode.WIN_DRAW_LOSE_SESSION;

        void incrementMode() {
            final Mode[] values = Mode.values();
            currentMode = values[(currentMode.ordinal() + 1) % values.length];
            updateDisplay();
            toToast(currentMode.toString());
        }

        void updateDisplay() {
            final TextView rockCount = (TextView) findViewById(R.id.rock_count);
            final TextView paperCount = (TextView) findViewById(R.id.paper_count);
            final TextView scissorsCount = (TextView) findViewById(R.id.scissors_count);
            final ImageView rockIcon = (ImageView) findViewById(R.id.rock_icon);
            final ImageView paperIcon = (ImageView) findViewById(R.id.paper_icon);
            final ImageView scissorsIcon = (ImageView) findViewById(R.id.scissors_icon);
            final ImageView winIcon = (ImageView) findViewById(R.id.win_icon);
            final ImageView drawIcon = (ImageView) findViewById(R.id.draw_icon);
            final ImageView loseIcon = (ImageView) findViewById(R.id.lose_icon);

            switch (currentMode) {
                case WIN_DRAW_LOSE_SESSION:
                    rockCount.setText(String.valueOf(model.getWinSession()));
                    paperCount.setText(String.valueOf(model.getDrawSession()));
                    scissorsCount.setText(String.valueOf(model.getLoseSession()));
                    rockIcon.setVisibility(View.GONE);
                    paperIcon.setVisibility(View.GONE);
                    scissorsIcon.setVisibility(View.GONE);
                    winIcon.setVisibility(View.VISIBLE);
                    drawIcon.setVisibility(View.VISIBLE);
                    loseIcon.setVisibility(View.VISIBLE);
                    break;
                case WIN_DRAW_LOSE_ALL_TIME:
                    rockCount.setText(String.valueOf(model.getWinAllTime()));
                    paperCount.setText(String.valueOf(model.getDrawAllTime()));
                    scissorsCount.setText(String.valueOf(model.getLoseAllTime()));
                    rockIcon.setVisibility(View.GONE);
                    paperIcon.setVisibility(View.GONE);
                    scissorsIcon.setVisibility(View.GONE);
                    winIcon.setVisibility(View.VISIBLE);
                    drawIcon.setVisibility(View.VISIBLE);
                    loseIcon.setVisibility(View.VISIBLE);
                    break;
                case ROCK_PAPER_SCISSORS_SESSION:
                    rockCount.setText(String.valueOf(model.getRockSession()));
                    paperCount.setText(String.valueOf(model.getPaperSession()));
                    scissorsCount.setText(String.valueOf(model.getScissorsSession()));
                    winIcon.setVisibility(View.GONE);
                    drawIcon.setVisibility(View.GONE);
                    loseIcon.setVisibility(View.GONE);
                    rockIcon.setVisibility(View.VISIBLE);
                    paperIcon.setVisibility(View.VISIBLE);
                    scissorsIcon.setVisibility(View.VISIBLE);
                    break;
                case ROCK_PAPER_SCISSORS_ALL_TIME:
                    rockCount.setText(String.valueOf(model.getRockAllTime()));
                    paperCount.setText(String.valueOf(model.getPaperAllTime()));
                    scissorsCount.setText(String.valueOf(model.getScissorsAllTime()));
                    winIcon.setVisibility(View.GONE);
                    drawIcon.setVisibility(View.GONE);
                    loseIcon.setVisibility(View.GONE);
                    rockIcon.setVisibility(View.VISIBLE);
                    paperIcon.setVisibility(View.VISIBLE);
                    scissorsIcon.setVisibility(View.VISIBLE);
                    break;
                case WIN_DRAW_LOSE_STREAK_SESSION:
                    rockCount.setText(String.valueOf(model.getWinStreakSession()));
                    paperCount.setText(String.valueOf(model.getDrawStreakSession()));
                    scissorsCount.setText(String.valueOf(model.getLoseStreakSession()));
                    rockIcon.setVisibility(View.GONE);
                    paperIcon.setVisibility(View.GONE);
                    scissorsIcon.setVisibility(View.GONE);
                    winIcon.setVisibility(View.VISIBLE);
                    drawIcon.setVisibility(View.VISIBLE);
                    loseIcon.setVisibility(View.VISIBLE);
                    break;
                case WIN_DRAW_LOSE_STREAK_ALL_TIME:
                    rockCount.setText(String.valueOf(model.getWinStreakAllTime()));
                    paperCount.setText(String.valueOf(model.getDrawStreakAllTime()));
                    scissorsCount.setText(String.valueOf(model.getLoseStreakAllTime()));
                    rockIcon.setVisibility(View.GONE);
                    paperIcon.setVisibility(View.GONE);
                    scissorsIcon.setVisibility(View.GONE);
                    winIcon.setVisibility(View.VISIBLE);
                    drawIcon.setVisibility(View.VISIBLE);
                    loseIcon.setVisibility(View.VISIBLE);
                    break;
                case ROCK_PAPER_SCISSORS_STREAK_SESSION:
                    rockCount.setText(String.valueOf(model.getRockStreakSession()));
                    paperCount.setText(String.valueOf(model.getPaperStreakSession()));
                    scissorsCount.setText(String.valueOf(model.getScissorsStreakSession()));
                    winIcon.setVisibility(View.GONE);
                    drawIcon.setVisibility(View.GONE);
                    loseIcon.setVisibility(View.GONE);
                    rockIcon.setVisibility(View.VISIBLE);
                    paperIcon.setVisibility(View.VISIBLE);
                    scissorsIcon.setVisibility(View.VISIBLE);
                    break;
                case ROCK_PAPER_SCISSORS_STREAK_ALL_TIME:
                    rockCount.setText(String.valueOf(model.getRockStreakAllTime()));
                    paperCount.setText(String.valueOf(model.getPaperStreakAllTime()));
                    scissorsCount.setText(String.valueOf(model.getScissorsStreakAllTime()));
                    winIcon.setVisibility(View.GONE);
                    drawIcon.setVisibility(View.GONE);
                    loseIcon.setVisibility(View.GONE);
                    rockIcon.setVisibility(View.VISIBLE);
                    paperIcon.setVisibility(View.VISIBLE);
                    scissorsIcon.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }
    }
}
