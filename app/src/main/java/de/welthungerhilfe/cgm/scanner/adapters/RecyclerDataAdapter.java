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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.models.Person;

public class RecyclerDataAdapter extends RecyclerView.Adapter<RecyclerDataAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Person> personList;
    private int lastPosition = -1;

    private OnPersonDetail personDetailListener;

    public RecyclerDataAdapter(Context ctx, ArrayList<Person> pl) {
        context = ctx;
        personList = pl;
    }

    @Override
    public RecyclerDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerDataAdapter.ViewHolder holder, int position) {
        Person person = personList.get(position);

        holder.txtName.setText(person.getName() + " " + person.getSurname());
        if (person.getMeasures() == null || person.getMeasures().size() == 0) {
            holder.txtWeight.setText(Float.toString(0f));
            holder.txtHeight.setText(Float.toString(0f));
        } else {
            holder.txtWeight.setText(Float.toString(person.getMeasures().get(person.getMeasures().size() - 1).getWeight()));
            holder.txtHeight.setText(Float.toString(person.getMeasures().get(person.getMeasures().size() - 1).getHeight()));
        }

        if (personDetailListener != null) {
            holder.bindPersonDetail(person);
        }

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return personList.size();
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
        notifyDataSetChanged();
    }

    public void addPerson(Person person) {
        personList.add(person);
        notifyItemInserted(personList.size() - 1);
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
}
