package fighter.tasks;

import com.runemate.game.api.hybrid.location.Coordinate;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum BossType {
    CRYSTALLINE_HUNLLEF(new Coordinate(1906, 5693, 1), new Coordinate(1917, 5682, 1)),
    CORRUPTED_HUNLLEF(new Coordinate(1970, 5693, 1), new Coordinate(1981, 5682, 1));

    private final Coordinate northwest;
    private final Coordinate southeast;

    BossType(Coordinate northwest, Coordinate southeast) {
        this.northwest = northwest;
        this.southeast = southeast;
    }

    // Checks if a given coordinate is within the boss room's boundaries
    public boolean isWithinBossRoom(Coordinate coordinate) {
        return coordinate.getX() >= northwest.getX() && coordinate.getX() <= southeast.getX() &&
                coordinate.getY() >= southeast.getY() && coordinate.getY() <= northwest.getY() &&
                coordinate.getPlane() == northwest.getPlane();
    }

    // Generates a grid of coordinates for the entire boss room area
    public Iterable<Coordinate> getRoomCoordinates() {
        List<Coordinate> coordinates = new ArrayList<>();
        for (int x = northwest.getX(); x <= southeast.getX(); x++) {
            for (int y = southeast.getY(); y <= northwest.getY(); y++) {
                coordinates.add(new Coordinate(x, y, northwest.getPlane()));
            }
        }
        return coordinates;
    }
}
