package uk.ac.cam.sp715.flows;

import uk.ac.cam.sp715.flows.Flow;
import uk.ac.cam.sp715.recipes.Recipe;

/**
 * Created by Srijan on 26/11/2015.
 */
public abstract class Visualiser {
    public abstract Flow parse(Recipe recipe);
}
