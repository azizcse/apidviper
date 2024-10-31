package com.w3engineers.mesh.application.data;
 
/*
============================================================================
Copyright (C) 2019 W3 Engineers Ltd. - All Rights Reserved.
Unauthorized copying of this file, via any medium is strictly prohibited
Proprietary and confidential
============================================================================
*/

import com.w3engineers.mesh.application.data.model.BroadcastEvent;
import com.w3engineers.mesh.application.data.model.DataAckEvent;
import com.w3engineers.mesh.application.data.model.DataEvent;
import com.w3engineers.mesh.application.data.model.Event;
import com.w3engineers.mesh.application.data.model.FilePendingEvent;
import com.w3engineers.mesh.application.data.model.FileProgressEvent;
import com.w3engineers.mesh.application.data.model.FileReceivedEvent;
import com.w3engineers.mesh.application.data.model.FileTransferEvent;
import com.w3engineers.mesh.application.data.model.PeerAdd;
import com.w3engineers.mesh.application.data.model.PeerRemoved;
import com.w3engineers.mesh.application.data.model.PermissionInterruptionEvent;
import com.w3engineers.mesh.application.data.model.ServiceDestroyed;
import com.w3engineers.mesh.application.data.model.ServiceUpdate;
import com.w3engineers.mesh.application.data.model.TransportInit;
import com.w3engineers.mesh.application.data.model.UserInfoEvent;
import com.w3engineers.mesh.application.data.model.WalletCreationEvent;
import com.w3engineers.mesh.application.data.model.WalletLoaded;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public interface ApiEvent {

    Class DATA = DataEvent.class;
    Class DATA_ACKNOWLEDGEMENT = DataAckEvent.class;

    Class PEER_ADD = PeerAdd.class;
    Class PEER_REMOVED = PeerRemoved.class;

    Class TRANSPORT_INIT = TransportInit.class;
    Class WALLET_LOADED = WalletLoaded.class;

    Class USER_INFO = UserInfoEvent.class;
    Class SERVICE_UPDATE = ServiceUpdate.class;
    Class PERMISSION_INTERRUPTION = PermissionInterruptionEvent.class;
    Class WALLET_CREATION_EVENT = WalletCreationEvent.class;

    // File message section
    Class FILE_PROGRESS_EVENT = FileProgressEvent.class;
    Class FILE_RECEIVED_EVENT = FileReceivedEvent.class;
    Class FILE_TRANSFER_EVENT = FileTransferEvent.class;
    Class SERVICE_DESTROYED = ServiceDestroyed.class;
    Class FILE_PENDING_EVENT = FilePendingEvent.class;
    Class BROADCAST_EVENT = BroadcastEvent.class;

    Disposable startObserver(Class event, Consumer<? extends Event> next);

    void sendObserverData(Event event);
}
