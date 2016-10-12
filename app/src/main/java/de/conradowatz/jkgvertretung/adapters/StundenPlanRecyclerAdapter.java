package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;


public class StundenPlanRecyclerAdapter extends RecyclerView.Adapter<StundenPlanRecyclerAdapter.ViewHolder> {

    private static final int MODE_STUNDENPLAN_OFFLINE = 0;
    private static final int MODE_STUNDENPLAN = 1;
    private static final int MODE_VERTRETUNGSPLAN = 2;
    private static final int MODE_ALLGSTUNDENPLAN = 3;
    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_STUNDENITEM = 2;
    private static final int VIEWTYPE_SILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private static final int VIEWTYPE_NOOFFLINE = 5;
    private static final int VIEWTYPE_FERIEN = 6;
    private Date date;
    private String datumString;
    private String zeitStempelString;
    private List<Stunde> stundenList = null;
    private List<Vertretung> vertretungsList = null;
    private int mode;
    private boolean noPlan;
    private boolean isFerien;
    private Callback callback;

    //OnlineStundenplan && Vertretungsplan
    private StundenPlanRecyclerAdapter(Tag tag, List<Stunde> stundenList, ArrayList<Vertretung> vertretungsList, int mode, boolean noPlan, Callback callback) {

        this.mode = mode;
        this.stundenList = stundenList;
        this.vertretungsList = vertretungsList;
        this.noPlan = noPlan;
        this.isFerien = VertretungsAPI.isntSchoolDay(tag.getDatum());
        this.callback = callback;
        this.date = tag.getDatum();

        datumString = tag.getDatumString();
        zeitStempelString = tag.getZeitStempel();
    }

    //OfflineStundenplan
    private StundenPlanRecyclerAdapter(Date datum, List<Stunde> stundenList, int mode, boolean isFerien, Callback callback) {

        this.mode = mode;
        this.stundenList = stundenList;
        this.isFerien = isFerien;
        this.noPlan = stundenList.size() == 0;
        this.callback = callback;
        this.date = datum;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datum);
        String wochenString = (LocalData.getInstance().isAWoche(datum)) ? " (A-Woche)" : " (B-Woche)";
        datumString = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN) + ", " + calendar.get(Calendar.DAY_OF_MONTH) + ". " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN) + " " + calendar.get(Calendar.YEAR) + wochenString;
    }

    public static StundenPlanRecyclerAdapter newOnlineStundenplanInstance(Tag tag, int klasseIndex, ArrayList<String> nichtKurse, Callback callback) {

        ArrayList<Stunde> stundenList = new ArrayList<>();
        if (tag.getStuplaKlasseList().size() > klasseIndex) {
            StuPlaKlasse stuPlaKlasse = tag.getStuplaKlasseList().get(klasseIndex);
            for (Stunde stunde : stuPlaKlasse.getStundenList()) {

                if (nichtKurse.contains(stunde.getKurs())
                        || nichtKurse.contains(stunde.getFach())
                        || nichtKurse.contains(stunde.getInfo().split(" ")[0]))
                    continue;

                stundenList.add(stunde);
            }
        }
        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0, callback);

    }

    public static StundenPlanRecyclerAdapter newOfflineStundenplanInstance(Date datum, Callback callback) {

        int dayOfWeek = Utilities.getDayOfWeek(datum);
        List<Stunde> stundenList;
        boolean isFerien = VertretungsAPI.isntSchoolDay(datum);
        if (isFerien) stundenList = new ArrayList<>();
        else
            stundenList = LocalData.getOfflineStundenList(dayOfWeek, LocalData.getInstance().isAWoche(datum));

        return new StundenPlanRecyclerAdapter(datum, stundenList, MODE_STUNDENPLAN_OFFLINE, isFerien, callback);

    }

    public static StundenPlanRecyclerAdapter newKlassenplanplanInstance(Tag tag, int klasseIndex) {

        ArrayList<Stunde> stundenList = new ArrayList<>();
        if (tag.getStuplaKlasseList().size() > klasseIndex) {
            stundenList = tag.getStuplaKlasseList().get(klasseIndex).getStundenList();
        }
        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0, null);

    }

    public static StundenPlanRecyclerAdapter newVertretungsplanInstance(Tag tag, String klassenString, ArrayList<String> nichtKurse) {

        ArrayList<Vertretung> vertretungsList = new ArrayList<>();
        for (Vertretung vertretung : tag.getVertretungsList()) {

            if (!vertretung.getKlasse().contains(klassenString))
                continue;

            boolean goOn = true;
            for (String nichtKursString : nichtKurse) {
                if (vertretung.getKlasse().contains(nichtKursString)) {
                    goOn = false;
                    break;
                }
            }
            if (!goOn) continue;

            vertretungsList.add(vertretung);

        }

        return new StundenPlanRecyclerAdapter(tag, null, vertretungsList, MODE_VERTRETUNGSPLAN, vertretungsList.size() == 0, null);

    }

    public static StundenPlanRecyclerAdapter newAllgvertretungsplanInstance(Tag tag) {

        return new StundenPlanRecyclerAdapter(tag, null, tag.getVertretungsList(), MODE_ALLGSTUNDENPLAN, tag.getVertretungsList().size() == 0, null);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == VIEWTYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_header, parent, false);
        } else if (viewType == VIEWTYPE_STUNDENITEM) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_stunde, parent, false);
        } else if (viewType == VIEWTYPE_SILAST) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_stunde_last, parent, false);
        } else if (viewType == VIEWTYPE_TEXT) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_text, parent, false);
        } else if (viewType == VIEWTYPE_NOOFFLINE) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_nooffline, parent, false);
        } else {  //viewtype == VIEWTYPE_FERIEN
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_ferien, parent, false);
        }

        return new ViewHolder(v, viewType);
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) return VIEWTYPE_HEADER;
        else {
            if (noPlan) {
                if (mode == MODE_STUNDENPLAN_OFFLINE || mode == MODE_STUNDENPLAN) {
                    return isFerien ? VIEWTYPE_FERIEN : VIEWTYPE_NOOFFLINE;
                } else return VIEWTYPE_TEXT;
            }
            if (position != getItemCount() - 1) return VIEWTYPE_STUNDENITEM;
            else return VIEWTYPE_SILAST;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == VIEWTYPE_HEADER) {

            holder.datumText.setText(datumString);
            if (mode == MODE_STUNDENPLAN_OFFLINE)
                holder.zeitstempelText.setText("erstellt aus Offlinedaten");
            else holder.zeitstempelText.setText("aktualisiert am " + zeitStempelString);

        } else if (viewType == VIEWTYPE_TEXT) {

            switch (mode) {
                case MODE_STUNDENPLAN:
                    holder.text.setText("Für diesen Tag wurden keine Stunden gefunden.");
                    break;
                case MODE_VERTRETUNGSPLAN:
                    holder.text.setText("Für diesen Tag wurde keine Vertretung gefunden. Auf dem allgemeinen Vertretungsplan könnten möglicherweise trotzdem Informationen für dich stehen.");
                    break;
                case MODE_ALLGSTUNDENPLAN:
                    holder.text.setText("Für diesen Tag steht kein Vertretungsplan bereit.");
                    break;
            }
        } else if (viewType == VIEWTYPE_NOOFFLINE) {

            holder.managerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onManagerClicked();
                }
            });

        } else if (viewType == VIEWTYPE_FERIEN) {

            String ferienName = "Schulfrei";
            String dateString = "";
            Ferien ferien = null;
            int ferienIndex = -1;
            Calendar cDate = Calendar.getInstance();
            cDate.setTime(date);
            for (int i = 0; i < LocalData.getInstance().getFerien().size(); i++) {
                Ferien f = LocalData.getInstance().getFerien().get(i);
                Calendar cStart = Calendar.getInstance();
                cStart.setTime(f.getStartDate());
                Calendar cEnd = Calendar.getInstance();
                cEnd.setTime(f.getEndDate());

                if (Utilities.compareDays(cDate, cStart) >= 0 && Utilities.compareDays(cDate, cEnd) <= 0) {
                    ferienName = f.getName();
                    dateString = String.format(Locale.GERMANY, "vom %d. %s %d\nbis %d. %s %d",
                            cStart.get(Calendar.DAY_OF_MONTH), cStart.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN), cStart.get(Calendar.YEAR),
                            cEnd.get(Calendar.DAY_OF_MONTH), cEnd.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN), cEnd.get(Calendar.YEAR));
                    ferien = f;
                    ferienIndex = i;
                    break;
                }
            }

            holder.feriennameText.setText(ferienName);
            if (ferien != null) {
                holder.dateText.setVisibility(View.VISIBLE);
                holder.dateText.setText(dateString);
                final int finalFerienIndex = ferienIndex;
                holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onFerienClicked(finalFerienIndex);
                    }
                });
            } else holder.dateText.setVisibility(View.GONE);

        } else { //VIEWTYPE_STUNDENITEM

            final Context context = holder.stundeText.getContext();

            if (mode == MODE_STUNDENPLAN || mode == MODE_STUNDENPLAN_OFFLINE) {

                holder.kursText.setVisibility(View.GONE);
                Stunde stunde = stundenList.get(position - 1);
                holder.stundeText.setText(stunde.getStunde());
                holder.fachText.setText(stunde.getFach());
                if (stunde.getFach().trim().isEmpty())
                    holder.fachText.setVisibility(View.GONE);
                else
                    holder.fachText.setVisibility(View.VISIBLE);
                if (stunde.isFachg())
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                holder.raumText.setText(stunde.getRaum());
                if (stunde.isRaumg())
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                if (stunde.getInfo().isEmpty())
                    holder.infoText.setVisibility(View.GONE);
                else {
                    holder.infoText.setVisibility(View.VISIBLE);
                    holder.infoText.setText(stunde.getInfo());
                }

                try {
                    final int stundenInt = Integer.valueOf(stunde.getStunde()); //1-9 erwartet
                    final String stundenName = stunde.getKurs() != null ? stunde.getKurs() : stunde.getFach();
                    final boolean isAWoche = LocalData.getInstance().isAWoche(date);
                    final int wochenTagInt = Utilities.getDayOfWeek(date); //1-5
                    Fach fach = null;
                    for (Fach f : LocalData.getInstance().getFächer()) {
                        if (f.getStunden(isAWoche)[wochenTagInt - 1][stundenInt - 1]) {
                            fach = f;
                            break;
                        }
                    }

                    Event event = null;
                    if (fach != null) {
                        for (Event e : fach.getEvents()) {
                            if (Utilities.compareDays(date, e.getDatum()) == 0) {
                                event = e;
                                holder.eventText.setText(e.getTitle());
                                break;
                            }
                        }
                    }
                    if (event == null) holder.eventText.setVisibility(View.GONE);
                    else holder.eventText.setVisibility(View.VISIBLE);

                    final Fach finalFach = fach;
                    final Event finalEvent = event;
                    holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (finalFach != null) {
                                if (finalEvent != null)
                                    callback.onEventClicked(finalFach, finalEvent, date);
                                else callback.onFachClicked(finalFach, date);
                            } else
                                callback.onNewStundeClicked(stundenName, isAWoche, wochenTagInt - 1, stundenInt - 1);
                        }
                    });
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }


            } else {

                Vertretung vertretung = vertretungsList.get(position - 1);
                holder.stundeText.setText(vertretung.getStunde());
                holder.fachText.setText(vertretung.getFach());
                if (vertretung.getFach().trim().isEmpty())
                    holder.fachText.setVisibility(View.GONE);
                else
                    holder.fachText.setVisibility(View.VISIBLE);
                holder.raumText.setText(vertretung.getRaum());
                if (vertretung.getInfo().isEmpty())
                    holder.infoText.setVisibility(View.GONE);
                else
                    holder.infoText.setVisibility(View.VISIBLE);
                holder.infoText.setText(vertretung.getInfo());
                holder.kursText.setText(String.format("%s:", vertretung.getKlasse()));

            }
        }
    }

    @Override
    public int getItemCount() {

        int itemCount;
        if (mode == MODE_STUNDENPLAN || mode == MODE_STUNDENPLAN_OFFLINE)
            itemCount = stundenList.size();
        else itemCount = vertretungsList.size();
        itemCount++;
        if (itemCount == 1) itemCount++;
        return itemCount;
    }

    public interface Callback {

        void onFachClicked(Fach fach, Date date);

        void onNewStundeClicked(String kursName, boolean aWoche, int tagIndex, int stundeIndex);

        void onManagerClicked();

        void onFerienClicked(int ferienIndex);

        void onEventClicked(Fach fach, Event event, Date date);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;

        TextView text;

        Button managerButton;

        TextView feriennameText;
        TextView dateText;

        LinearLayout linearLayout;

        TextView kursText;
        TextView stundeText;
        TextView fachText;
        TextView raumText;
        TextView infoText;
        TextView eventText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);

            } else if (viewType == VIEWTYPE_TEXT) {

                text = (TextView) itemView.findViewById(R.id.text);

            } else if (viewType == VIEWTYPE_NOOFFLINE) {

                managerButton = (Button) itemView.findViewById(R.id.managerButton);

            } else if (viewType == VIEWTYPE_FERIEN) {

                linearLayout = (LinearLayout) itemView.findViewById(R.id.linearLayout);
                feriennameText = (TextView) itemView.findViewById(R.id.feriennameText);
                dateText = (TextView) itemView.findViewById(R.id.dateText);

            } else {

                linearLayout = (LinearLayout) itemView.findViewById(R.id.linearLayout);
                kursText = (TextView) itemView.findViewById(R.id.kursText);
                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                raumText = (TextView) itemView.findViewById(R.id.raumText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
                eventText = (TextView) itemView.findViewById(R.id.eventText);
            }
        }
    }
}
