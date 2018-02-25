/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class RecyclerDataAdapter extends RecyclerView.Adapter<RecyclerDataAdapter.ViewHolder> implements Filterable {
    private Context context;
    private ArrayList<Person> personList;
    private ArrayList<Person> filteredList;
    private int lastPosition = -1;

    private int sortType = 0; // 0 : All, 1 : date, 2 : location, 3 : wasting, 4 : stunting;
    private long startDate, endDate;
    private Loc currentLoc;
    private int radius;

    private PersonFilter personFilter = new PersonFilter();

    private OnPersonDetail personDetailListener;

    public RecyclerDataAdapter(Context ctx, ArrayList<Person> pl) {
        context = ctx;
        personList = pl;
        filteredList = pl;
    }

    @Override
    public RecyclerDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerDataAdapter.ViewHolder holder, int position) {
        Person person = filteredList.get(position);

        holder.txtName.setText(person.getName() + " " + person.getSurname());
        if (person.getLastMeasure() == null) {
            holder.txtWeight.setText(Float.toString(0f));
            holder.txtHeight.setText(Float.toString(0f));
        } else {
            holder.txtWeight.setText(Float.toString(person.getLastMeasure().getWeight()));
            holder.txtHeight.setText(Float.toString(person.getLastMeasure().getHeight()));
        }

        if (personDetailListener != null) {
            holder.bindPersonDetail(person);
        }

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void setPersonDetailListener(OnPersonDetail listener) {
        personDetailListener = listener;
    }

    public void resetData(ArrayList<Person> personList) {
        this.personList = personList;
        getFilter().filter("");
    }

    public void addPerson(Person person) {
        personList.add(person);
        getFilter().filter("");
    }

    public void setDateFilter(long start, long end) {
        startDate = start;
        endDate = end;
        sortType = 1;

        getFilter().filter("");
    }

    public void setLocationFilter(Loc loc, int r) {
        currentLoc = loc;
        radius = r;
        sortType = 2;

        getFilter().filter("");
    }

    public void setWastingFilter() {
        sortType = 3;

        getFilter().filter("");
    }

    public void setStuntingFilter() {
        sortType = 4;

        getFilter().filter("");
    }

    public void clearFitlers() {
        sortType = 0;

        getFilter().filter("");
    }

    public void search(CharSequence query) {
        sortType = 5;

        getFilter().filter(query);
    }

    @Override
    public Filter getFilter() {
        return personFilter;
    }

    public void emptyData() {
        personList = new ArrayList<>();
    }

    public void updatePerson(Person person) {
        int index = -1;
        for (int i = 0; i < personList.size(); i++) {
            if (person.getId().equals(personList.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            personList.remove(index);
            personList.add(index, person);
        }
        getFilter().filter("");
    }

    public void removePerson(Person person) {
        int index = -1;
        for (int i = 0; i < personList.size(); i++) {
            if (person.getId().equals(personList.get(i).getId())) {
                index = i;
                break;
            }
        }
        if (index > -1) {
            personList.remove(index);
        }
        getFilter().filter("");
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout rytItem;

        public TextView txtName;
        public TextView txtWeight;
        public TextView txtHeight;

        public ViewHolder(View itemView) {
            super(itemView);

            rytItem = itemView.findViewById(R.id.rytItem);

            txtName = itemView.findViewById(R.id.txtName);
            txtWeight = itemView.findViewById(R.id.txtWeight);
            txtHeight = itemView.findViewById(R.id.txtHeight);
        }

        public void bindPersonDetail(final Person person) {
            rytItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    personDetailListener.onPersonDetail(person);
                }
            });
        }
    }

    public interface OnPersonDetail {
        void onPersonDetail(Person person);
    }

    public class PersonFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();

            ArrayList<Person> tempList = new ArrayList<>();
            for (int i = 0; i < personList.size(); i++) {
                if (sortType == 1) {
                    if (personList.get(i).getCreated() <= endDate && personList.get(i).getCreated() >= startDate)
                        tempList.add(personList.get(i));
                } else if (sortType == 2) {
                    if (currentLoc == null || personList.get(i).getLastLocation() == null) {

                    } else if (Utils.distanceBetweenLocs(currentLoc, personList.get(i).getLastLocation()) < radius)
                        tempList.add(personList.get(i));
                } else if (sortType == 5) {
                    if (personList.get(i).getName().contains(charSequence) || personList.get(i).getSurname().contains(charSequence)) {
                        tempList.add(personList.get(i));
                    }
                } else {
                    tempList.add(personList.get(i));
                }
            }

            if (sortType == 0 || sortType == 5) { // No filter

            } else {
                Collections.sort(tempList, new Comparator<Person>() {
                    @Override
                    public int compare(Person person, Person t1) {
                        if (sortType == 1) {   // Sort by created date
                            return person.getCreated() > t1.getCreated() ? 1 : person.getCreated() < t1.getCreated() ? -1 : 0;
                        } else if (sortType == 2) {   // Sort by distance from me
                            if (currentLoc == null || person.getLastLocation() == null)
                                return  0;
                            return Utils.distanceBetweenLocs(currentLoc, person.getLastLocation()) > Utils.distanceBetweenLocs(currentLoc, t1.getLastLocation()) ? 1 : Utils.distanceBetweenLocs(currentLoc, person.getLastLocation()) < Utils.distanceBetweenLocs(currentLoc, t1.getLastLocation()) ? -1 : 0;
                        } else if (sortType == 3) {   // Sort by wasting
                            if (person.getLastMeasure() == null)
                                return 0;
                            return person.getLastMeasure().getWeight() / person.getLastMeasure().getHeight() > t1.getLastMeasure().getWeight() / t1.getLastMeasure().getHeight() ? -1 : person.getLastMeasure().getWeight() / person.getLastMeasure().getHeight() < t1.getLastMeasure().getWeight() / t1.getLastMeasure().getHeight() ? 1 : 0;
                        } else if (sortType == 4) {   // sort by stunting
                            if (person.getLastMeasure() == null)
                                return 0;

                            return person.getLastMeasure().getHeight() / person.getLastMeasure().getAge() > t1.getLastMeasure().getHeight() / t1.getLastMeasure().getAge() ? -1 : person.getLastMeasure().getHeight() / person.getLastMeasure().getAge() < t1.getLastMeasure().getHeight() / t1.getLastMeasure().getAge() ? 1 : 0;
                        }
                        return 0;
                    }
                });
            }

            results.values = tempList;
            results.count = tempList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults results) {
            filteredList = (ArrayList<Person>) results.values;
            notifyDataSetChanged();
        }
    }
}
