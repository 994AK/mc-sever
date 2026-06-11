package net.leafmc.friends;

public final class FriendSettings {
    private boolean receiveRequests = true;
    private boolean receiveTeleports = true;
    private boolean receiveMessages = true;
    private boolean onlineNotifications = true;
    private boolean showOnlineStatus = true;

    public boolean receiveRequests() {
        return receiveRequests;
    }

    public void setReceiveRequests(boolean receiveRequests) {
        this.receiveRequests = receiveRequests;
    }

    public boolean receiveTeleports() {
        return receiveTeleports;
    }

    public void setReceiveTeleports(boolean receiveTeleports) {
        this.receiveTeleports = receiveTeleports;
    }

    public boolean receiveMessages() {
        return receiveMessages;
    }

    public void setReceiveMessages(boolean receiveMessages) {
        this.receiveMessages = receiveMessages;
    }

    public boolean onlineNotifications() {
        return onlineNotifications;
    }

    public void setOnlineNotifications(boolean onlineNotifications) {
        this.onlineNotifications = onlineNotifications;
    }

    public boolean showOnlineStatus() {
        return showOnlineStatus;
    }

    public void setShowOnlineStatus(boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }

}
