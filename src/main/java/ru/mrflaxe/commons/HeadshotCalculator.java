package ru.mrflaxe.commons;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Useful class for calculation headshot hits
 * @author Mrflaxe
 */
public class HeadshotCalculator {
    
    private static final float humanHeight = 1.8f;
    private static final float eyesHeight = 1.6200000047683716f;
    private static final float playerSneakingHeight = 1.5f;
    private static final float eyesSneakingHeight = 1.2699999809265137f;
    
    /*
     * Works only for player's melee attacks
     * This code is perfect.
     * If a bug is found, only me can fix it.
     * I'm the creator of this abomination.
     * And I'm the one who can understand here something.
     *
     * I mean, this is really hard to understand without my notes and formulas.
     */
    
    /**
     * Calculates does player melee hit the head part of another player or not.
     * Works for any distance.
     * @return true if hit was directed at the head. Otherwise false
     */
    public static boolean isHeadshot(Player attacker, Player victim) {
        Location attackerLoc = attacker.getLocation();
        Location victimLoc = victim.getLocation();
        
        if (victim.isSleeping()) {
            return false;
        }
        
        if (victim.isGliding()) {
            return false;
        }
        
        if (victim.isSwimming()) {
            return false;
        }
        
        if (victim.isOnGround()) {
            return false;
        }
        
        float pitch = attackerLoc.getPitch();
        float yaw = Math.abs(attackerLoc.getYaw()) - 180;
        
        double playersYDifference = attackerLoc.getY() - victimLoc.getY();
        double distance = get2DimensionDistance(attackerLoc, victimLoc);
        
        // Check did the player hit the base or the upper face of a cuboid hitbox
        if (Math.abs(playersYDifference) > 0) {
            
            Location eyeLocation = attacker.getEyeLocation();
            double x = eyeLocation.getX();
            double z = eyeLocation.getZ();
            double y = eyeLocation.getY();
            
            double yProportion = Math.abs(pitch/90);
            double xProportion, zProportion;
            
            double absYaw = Math.abs(yaw);
            if (absYaw > 90) {
                zProportion = absYaw % 90 / 90;
                xProportion = 1 - zProportion;
            } else {
                xProportion = absYaw / 90;
                zProportion = 1 - xProportion;
            }
            
            double baseY = victimLoc.getY();
            double upperFaceY;
            
            if (victim.isSneaking()) {
                upperFaceY = victimLoc.getY() + playerSneakingHeight;
            } else {
                upperFaceY = victimLoc.getY() + humanHeight;
            }
            
            double baseYDifference = Math.abs(baseY - y);
            double upperFaceYDifference = Math.abs(upperFaceY - y);
            
            double xzPieceForBase = baseYDifference / yProportion - baseYDifference;
            double xzPieceForUpperFace = upperFaceYDifference / yProportion - upperFaceYDifference;
            
            
            double xDifferenceForBase = xProportion * xzPieceForBase;
            double xDifferenceForUpperFace = xProportion * xzPieceForUpperFace;
            double zDifferenceForBase = zProportion * xzPieceForBase;
            double zDifferenceForUpperFace = zProportion * xzPieceForUpperFace;
            
            xDifferenceForBase += xDifferenceForBase * 73.2 / 100;
            xDifferenceForUpperFace += xDifferenceForUpperFace * 73.2 / 100;
            zDifferenceForBase += zDifferenceForBase * 73.2 / 100;
            zDifferenceForUpperFace += zDifferenceForUpperFace * 73.2 / 100;
            
            double resultZForBase, resultZForUpperFace, resultXForBase, resultXForUpperFace;
            
            if (Math.abs(yaw) < 90) {
                resultZForBase = z - zDifferenceForBase;
                resultZForUpperFace = z - zDifferenceForUpperFace;
            } else {
                resultZForBase = z + zDifferenceForBase;
                resultZForUpperFace = z + zDifferenceForUpperFace;
            }
            
            if (yaw > 0) {
                resultXForBase = x - xDifferenceForBase;
                resultXForUpperFace = x - xDifferenceForUpperFace;
            } else {
                resultXForBase = x + xDifferenceForBase;
                resultXForUpperFace = x + xDifferenceForUpperFace;
            }
            
            // check if the result location belongs to hitbox face
            
            double lowerHitboxX = victimLoc.getX() - 0.3;
            double higherHitboxX = victimLoc.getX() + 0.3;
            double lowerHitboxZ = victimLoc.getZ() - 0.3;
            double higherHitboxZ = victimLoc.getZ() + 0.3;
            
            if (playersYDifference > 0) {
                boolean headshot = !(resultXForUpperFace > higherHitboxX) && !(resultXForUpperFace < lowerHitboxX);
                
                if (resultZForUpperFace > higherHitboxZ || resultZForUpperFace < lowerHitboxZ) {
                    headshot = false;
                }
                
                if (headshot) {
                    return true;
                }
            }
            
            boolean legsHit = !(resultXForBase > higherHitboxX) && !(resultXForBase < lowerHitboxX);
            
            if (resultZForBase > higherHitboxX || resultZForBase < lowerHitboxZ) {
                legsHit = false;
            }
            
            if (legsHit) {
                return false;
            }
        }
        
        // If that isn't a base plate or upper face hit,
        // We have to calculate a ray and check does it cross the second player's hitbox.
        double aSegment = getASegment(pitch, distance);
        
        double resultEyesHeight = attacker.isSneaking() ? eyesSneakingHeight : eyesHeight;
        
        double resultHitHeight;
        if (pitch < 0) {
            resultHitHeight = resultEyesHeight + aSegment + playersYDifference;
        } else {
            resultHitHeight = resultEyesHeight - aSegment + playersYDifference;
        }
        
        if (victim.isSneaking()) {
            return resultHitHeight > playerSneakingHeight - 0.5;
        }
        
        return resultHitHeight > humanHeight - 0.5;
    }
    
    private static double getASegment(float pitch, double distance) {
        float angle;
        
        if (pitch < 0) {
            angle = 180 - 90 - pitch;
        } else {
            angle = 90 - pitch;
        }
        
        double radians = Math.toRadians(angle);
        double alpha = Math.sin(radians);
        
        double pitchDistance = distance / alpha;
        
        double powSub = Math.pow(pitchDistance, 2) - Math.pow(distance, 2);
        return Math.sqrt(powSub);
    }
    
    /**
     * Return the distance between two locations only in two dimensions (horizontal plane).
     * Y dimension isn't taken into account.
     * @return the distance between two locations only in two dimensions (horizontal plane)
     */
    private static double get2DimensionDistance(Location firstLoc, Location secondLoc) {
        double firstX = firstLoc.getX();
        double secondX = secondLoc.getX();
        double firstZ = firstLoc.getZ();
        double secondZ = secondLoc.getZ();
        
        double xDifference = Math.abs(firstX - secondX);
        double zDifference = Math.abs(firstZ - secondZ);
        
        double powSum = Math.pow(xDifference, 2) + Math.pow(zDifference, 2);
        return Math.sqrt(powSum);
    }
}