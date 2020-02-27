package iooogik.app.modelling.notes;

interface NoteInterface {
    void updateData(String databaseName, String name, String note, String shortNote);
    String getButtonName();
    int getButtonID();
    void updateFragment();
    void updateShopNotes(String databaseName, String name, String booleans);
    void alarmDialog(final String title, final String text);

}
