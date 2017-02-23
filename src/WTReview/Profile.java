/*
 * Copyright (c) 2017 S. Griffin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package WTReview;

import java.util.ArrayList;

public class Profile {

    private ArrayList<Double> xValues;
    private ArrayList<Double> yValues;

    public Profile(ArrayList<Double> xValues, ArrayList<Double> yValues) {

        if (xValues.size() != yValues.size()) {
            String error = String.format("Profile Data created with uneven dimensions: %s, %s", xValues.size(), yValues.size());
            System.out.println(error);
        }

        this.xValues = xValues;
        this.yValues = yValues;
    }

    public ArrayList<Double> getxValues() {
        return xValues;
    }

    public ArrayList<Double> getyValues() {
        return yValues;
    }
}
