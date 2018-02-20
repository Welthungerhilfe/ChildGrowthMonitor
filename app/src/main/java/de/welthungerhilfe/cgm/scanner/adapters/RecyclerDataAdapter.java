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

    public RecyclerDataAdapter(Context ctx, ArrayList<Person> pl) {
        context = ctx;
        personList = pl;
    }

    @Override
    public RecyclerDataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_data, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerDataAdapter.ViewHolder holder, int position) {
        /*
        Person person = personList.get(position);

        holder.txtName.setText(person.getName() + " " + person.getSurname());
        holder.txtWeight.setText(Float.toString(person.getMeasure().getWeight()));
        holder.txtHeight.setText(Float.toString(person.getMeasure().getHeight()));
        */

        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        //return personList.size();
        return 15;
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    public void resetData(ArrayList<Person> personList) {
        this.personList = personList;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txtName)
        TextView txtName;
        @BindView(R.id.txtWeight)
        TextView txtWeight;
        @BindView(R.id.txtHeight)
        TextView txtHeight;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(itemView);
        }
    }
}
