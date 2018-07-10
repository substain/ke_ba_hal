package hlt;

public class Constants {
/*
 * 
 * 
 * 
 * 
 * 

namespace hlt {
    constexpr auto MAX_PLAYERS = 2; //4
    constexpr auto MAX_QUEUED_MOVES = 1;

    struct GameConstants {
        int SHIPS_PER_PLAYER = 5; //3
        int PLANETS_PER_PLAYER = 6; //6
        unsigned int EXTRA_PLANETS = 6; //4
        unsigned int MAX_TURNS = 320; //300

        double DRAG = 10.0;
        double MAX_SPEED = 7.0;
        double MAX_ACCELERATION = 7.0;

        double SHIP_RADIUS = 0.5; //0.5

        unsigned short MAX_SHIP_HEALTH = 255; //255
        unsigned short BASE_SHIP_HEALTH = 255; //255
        unsigned short DOCKED_SHIP_REGENERATION = 1; //0

        unsigned int WEAPON_COOLDOWN = 2; //1
        double WEAPON_RADIUS = 4.0; //5.0
        int WEAPON_DAMAGE = 80; //64
        double EXPLOSION_RADIUS = 25.0; //10.0

        double DOCK_RADIUS = 4.5; //4.0
        unsigned int DOCK_TURNS = 6; //5
        int RESOURCES_PER_RADIUS = 180; //144
        bool INFINITE_RESOURCES = false; //true
        int PRODUCTION_PER_SHIP = 82; //72
        unsigned int BASE_PRODUCTIVITY = 6;
        unsigned int ADDITIONAL_PRODUCTIVITY = 6;

        int SPAWN_RADIUS = 3; //2

        static auto get_mut() -> GameConstants& {
            // Guaranteed initialized only once by C++11
            static GameConstants instance;
            return instance;
        }

        static auto get() -> const GameConstants& {
            return get_mut();
        }

        auto to_json() const -> nlohmann::json;
        auto from_json(const nlohmann::json& json) -> void;
    };
}
 * 
 * 
 */
    ////////////////////////////////////////////////////////////////////////
    // Implementation-independent language-agnostic constants

    /** Games will not have more players than this */
    public static final int MAX_PLAYERS = 4; //4

    /** Max number of units of distance a ship can travel in a turn */
    public static final int MAX_SPEED = 7; //7
    
    
    public static final int NAV_CRIT = 10;

    public static final double FLY_RANGE = MAX_SPEED * 2;

    /** Radius of a ship */
    public static final double SHIP_RADIUS = 0.5; //0.5

    /** Starting health of ship, also its max */
    public static final int MAX_SHIP_HEALTH = 255; //255

    /** Starting health of ship, also its max */
    public static final int BASE_SHIP_HEALTH = 255; //255

    /** Weapon cooldown period */
    public static final int WEAPON_COOLDOWN = 1; //1

    /** Weapon damage radius */
    public static final double WEAPON_RADIUS = 5.0; //5.0

    /** Weapon damage */
    public static final int WEAPON_DAMAGE = 64; //64

    /** Radius in which explosions affect other entities */
    public static final double EXPLOSION_RADIUS = 10.0; //10.0

    /** Distance from the edge of the planet at which ships can try to dock */
    public static final double DOCK_RADIUS = 4.0; //4.0


    /** Number of turns it takes to dock a ship */
    public static final int DOCK_TURNS = 5; //5

    /** Number of production units per turn contributed by each docked ship */
    public static final int BASE_PRODUCTIVITY = 6; //6

    /** Distance from the planets edge at which new ships are created */
    public static final double SPAWN_RADIUS = 2.0; //2.0

    ////////////////////////////////////////////////////////////////////////
    // Implementation-specific constants

    public static final double FORECAST_FUDGE_FACTOR = SHIP_RADIUS + 0.1; 
    public static final double FORECAST_FUDGE_FACTOR2 = SHIP_RADIUS + 0.4;

    public static final double FORECAST_FUDGE_FACTOR_DIV = SHIP_RADIUS * 10;

    //old: 90
    public static final int MAX_NAVIGATION_CORRECTIONS = 80; //90

    /**
     * Used in Position.getClosestPoint()
     * Minimum distance specified from the object's outer radius.
     */
    public static final int MIN_DISTANCE_FOR_CLOSEST_POINT = 3; //3
    
    
    
    public static final int MAX_BREAK_DISTANCE = MAX_SPEED + 1;
    
    public static final int MAX_NUM_ROUNDS = 300; //300
}
