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

import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class ViewHelpers {

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    public static <T> void setupDragDrop(ListView<T> listView) {
        listView.setCellFactory(tv -> {
            DraggableTextFieldListCell<T> cell = new DraggableTextFieldListCell<T>();

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Integer index = cell.getIndex();
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(cell.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (cell.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            cell.setOnDragEntered(event -> {
                if (!cell.isEmpty()) {
                    cell.setDraggedOver(true);
                }
                event.consume();
            });

            cell.setOnDragExited(event -> cell.setDraggedOver(false));

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    T draggedItem = listView.getItems().remove(draggedIndex);

                    int dropIndex;

                    if (cell.isEmpty()) {
                        dropIndex = listView.getItems().size();
                    } else {
                        dropIndex = cell.getIndex();
                    }

                    listView.getItems().add(dropIndex, draggedItem);

                    event.setDropCompleted(true);
                    listView.getSelectionModel().select(dropIndex);
                    event.consume();
                }
            });

            return cell;
        });
    }
}
