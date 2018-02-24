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

package de.welthungerhilfe.cgm.scanner.helper.events;

import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;

/**
 * Created by Emerald on 2/24/2018.
 */

public class LocationResult {
    private Loc location;

    public LocationResult(Loc location) {
        this.location = location;
    }

    public Loc getLocationResult() {
        return this.location;
    }
}
