/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests rearrangeing of a colony.
 */
public class RearrangeColonyMessage extends AttributeMessage {

    public static final String TAG = "rearrangeColony";
    private static final String COLONY_TAG = "colony";

    /** Container for the unit change information. */
    public static class Arrangement implements Comparable<Arrangement> {

        private Unit unit;
        private Location loc;
        private GoodsType work;
        private Role role;
        private int roleCount;



        public Arrangement() {} // deliberately empty

        public Arrangement(Unit unit, Location loc, GoodsType work,
                          Role role, int roleCount) {
            this.setUnit(unit);
            this.setLoc(loc);
            this.setWork(work);
            this.setRole(role);
            this.setRoleCount(roleCount);
        }

        public Arrangement(Game game, String unitId,
                          String locId, String workId,
                          String roleId, String roleCount) {
            init(game, unitId, locId, workId, roleId, roleCount);
        }

        public final void init(Game game, String unitId, 
                               String locId, String workId, 
                               String roleId, String roleCount) {
            this.setUnit(game.getFreeColGameObject(unitId, Unit.class));
            this.setLoc(game.findFreeColLocation(locId));
            this.setWork((workId == null || workId.isEmpty()) ? null
                : game.getSpecification().getGoodsType(workId));
            this.setRole(game.getSpecification().getRole(roleId));
            try {
                this.setRoleCount(Integer.parseInt(roleCount));
            } catch (NumberFormatException nfe) {
                this.setRoleCount(0);
            }
        }

        public static String unitKey(int i) {
            return FreeColObject.arrayKey(i) + "unit";
        }

        public static String locKey(int i) {
            return FreeColObject.arrayKey(i) + "loc";
        }

        public static String workKey(int i) {
            return FreeColObject.arrayKey(i) + "work";
        }

        public static String roleKey(int i) {
            return FreeColObject.arrayKey(i) + "role";
        }

        public static String roleCountKey(int i) {
            return FreeColObject.arrayKey(i) + "count";
        }

        /**
         * Create new arrangements for a given list of worker units on the
         * basis of a scratch colony configuration.
         *
         * @param colony The original {@code Colony}.
         * @param workers A list of worker {@code Unit}s to arrange.
         * @param scratch The scratch {@code Colony}.
         * @return A list of {@code Arrangement}s.
         */
        public static List<Arrangement> getArrangements(Colony colony,
                                                        List<Unit> workers,
                                                        Colony scratch) {
            List<Arrangement> ret = new ArrayList<>();
            for (Unit u : workers) {
                Unit su = scratch.getCorresponding(u);
                if (u.getLocation().getId().equals(su.getLocation().getId())
                    && u.getWorkType() == su.getWorkType()
                    && u.getRole() == su.getRole()
                    && u.getRoleCount() == su.getRoleCount()) continue;
                ret.add(new Arrangement(u,
                        (Location)colony.getCorresponding((FreeColObject)su.getLocation()),
                        su.getWorkType(), su.getRole(), su.getRoleCount()));
            }
            return ret;
        }

            
        // Interface Comparable<Arrangement>

        /**
         * {@inheritDoc}
         */
        public int compareTo(Arrangement other) {
            int cmp = this.getRole().compareTo(other.getRole());
            if (cmp == 0) cmp = this.getRoleCount() - other.getRoleCount();
            return cmp;
        }

        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "[Arrangement " + getUnit().getId() + " at " + getLoc().getId()
                + " " + getRole().getRoleSuffix() + "." + getRoleCount()
                + ((getWork() == null) ? "" : " work " + getWork().getId()) + "]";
        }

        public Unit getUnit() {
            return unit;
        }

        public void setUnit(Unit unit) {
            this.unit = unit;
        }

        public Location getLoc() {
            return loc;
        }

        public void setLoc(Location loc) {
            this.loc = loc;
        }

        public GoodsType getWork() {
            return work;
        }

        public void setWork(GoodsType work) {
            this.work = work;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public int getRoleCount() {
            return roleCount;
        }

        public void setRoleCount(int roleCount) {
            this.roleCount = roleCount;
        }
    }


    /**
     * Create a new {@code RearrangeColonyMessage} with the
     * supplied colony.  Add changes with addChange().
     *
     * @param colony The {@code Colony} that is rearranging.
     * @param workers A list of worker {@code Unit}s to rearrange.
     * @param scratch A scratch {@code Colony} laid out as required.
     */
    public RearrangeColonyMessage(Colony colony, List<Unit> workers,
                                  Colony scratch) {
        super(TAG, COLONY_TAG, colony.getId());

        setArrangementAttributes(Arrangement.getArrangements(colony, workers, scratch));
    }

    /**
     * Create a new {@code RearrangeColonyMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public RearrangeColonyMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, getAttributeMap(xr));
    }

    /**
     * Read the attributes from the stream.
     * 
     * @param xr The {@code FreeColXMLReader} to read from.
     * @return An attribute map.
     */
    private static Map<String, String> getAttributeMap(FreeColXMLReader xr) {
        Map<String, String> ret = new HashMap<>();
        ret.put(COLONY_TAG, xr.getAttribute(COLONY_TAG, (String)null));
        int n = xr.getAttribute(FreeColObject.ARRAY_SIZE_TAG, 0);
        for (int i = 0; i < n; i++) {
            ret.put(Arrangement.unitKey(i),
                xr.getAttribute(Arrangement.unitKey(i), (String)null));
            ret.put(Arrangement.locKey(i),
                xr.getAttribute(Arrangement.locKey(i), (String)null));
            ret.put(Arrangement.workKey(i),
                xr.getAttribute(Arrangement.workKey(i), (String)null));
            ret.put(Arrangement.roleKey(i),
                xr.getAttribute(Arrangement.roleKey(i), (String)null));
            ret.put(Arrangement.roleCountKey(i),
                xr.getAttribute(Arrangement.roleCountKey(i), (String)null));
        }
        return ret;
    }

    /**
     * Set the attributes consequent to a list of arrangements.
     *
     * @param arrangements The list of {@code Arrangement}.
     */
    private void setArrangementAttributes(List<Arrangement> arrangements) {
        int i = 0;
        for (Arrangement a : arrangements) {
            setStringAttribute(a.unitKey(i), a.getUnit().getId());
            setStringAttribute(a.locKey(i), a.getLoc().getId());
            if (a.getWork() != null) {
                setStringAttribute(a.workKey(i), a.getWork().getId());
            }
            setStringAttribute(a.roleKey(i), a.getRole().toString());
            setStringAttribute(a.roleCountKey(i), String.valueOf(a.getRoleCount()));
            i++;
        }
        setIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, i);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String colonyId = getStringAttribute(COLONY_TAG);
        final Game game = serverPlayer.getGame();
        final List<Arrangement> arrangements = getArrangements(game);
        
        Colony colony;
        try {
            colony = serverPlayer.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (arrangements.isEmpty()) {
            return serverPlayer.clientError("Empty rearrangement list.");
        }
        int i = 0;
        for (Arrangement uc : arrangements) {
            if (uc.getUnit() == null) {
                return serverPlayer.clientError("Invalid unit " + i);
            }
            if (uc.getLoc() == null) {
                return serverPlayer.clientError("Invalid location " + i);
            }
            if (uc.getRole() == null) {
                return serverPlayer.clientError("Invalid role " + i);
            }
            if (uc.getRoleCount() < 0) {
                return serverPlayer.clientError("Invalid role count " + i);
            }
        }

        // Rearrange can proceed.
        return igc(freeColServer)
            .rearrangeColony(serverPlayer, colony, arrangements);
    }


    // Public interface

    /**
     * Check if the are no arrangements present.
     *
     * @return True if there are no arrangements.
     */
    public boolean isEmpty() {
        return getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, 0) == 0;
    }

    /**
     * Get arrangements from the attributes.
     *
     * @param game The {@code Game} to create arrangements in.
     * @return A list of {@code Arrangement}s.
     */
    public List<Arrangement> getArrangements(Game game) {
        List<Arrangement> ret = new ArrayList<>();
        int n = getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, 0);
        for (int i = 0; i < n; i++) {
            ret.add(new Arrangement(game,
                                    getStringAttribute(Arrangement.unitKey(i)),
                                    getStringAttribute(Arrangement.locKey(i)),
                                    getStringAttribute(Arrangement.workKey(i)),
                                    getStringAttribute(Arrangement.roleKey(i)),
                                    getStringAttribute(Arrangement.roleCountKey(i))));
        }
        return ret;
    }
}
