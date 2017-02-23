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

public class MeasurementPoint {
    //*abs time, delta time, iec x/y, iec z, tt1, tt2, tt3, tt4, tt5, tt6, tt7, tt8
    private double absTime;
    private double deltaTime;
    private double lateralPos;
    private double verticalPos;
    private double channel1;
    private double channel2;
    private double channel3;
    private double channel4;
    private double channel5;
    private double channel6;
    private double channel7;
    private double channel8;

    public MeasurementPoint(double absTime, double deltaTime, double lateralPos, double verticalPos, double channel1, double channel2, double channel3, double channel4, double channel5, double channel6, double channel7, double channel8) {
        this.absTime = absTime;
        this.deltaTime = deltaTime;
        this.lateralPos = lateralPos;
        this.verticalPos = verticalPos;
        this.channel1 = channel1;
        this.channel2 = channel2;
        this.channel3 = channel3;
        this.channel4 = channel4;
        this.channel5 = channel5;
        this.channel6 = channel6;
        this.channel7 = channel7;
        this.channel8 = channel8;
    }


    public double getAbsTime() {
        return absTime;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public double getLateralPos() {
        return lateralPos;
    }

    public double getVerticalPos() {
        return verticalPos;
    }

    public double getChannel1() {
        return channel1;
    }

    public double getChannel2() {
        return channel2;
    }

    public double getChannel3() {
        return channel3;
    }

    public double getChannel4() {
        return channel4;
    }

    public double getChannel5() {
        return channel5;
    }

    public double getChannel6() {
        return channel6;
    }

    public double getChannel7() {
        return channel7;
    }

    public double getChannel8() {
        return channel8;
    }
}
