package com.example.splendormobilegame.activities.GameActivity;
import com.example.splendormobilegame.model.Game;
import com.example.splendormobilegame.model.Model;
import android.util.Log;

import com.example.splendormobilegame.Controller;
import com.example.splendormobilegame.model.Room;
import com.example.splendormobilegame.model.User;
import com.example.splendormobilegame.websocket.CustomWebSocketClient;
import com.example.splendormobilegame.websocket.ReactionUtils;
import com.example.splendormobilegame.websocket.UserReaction;
import com.github.splendor_mobile_game.websocket.communication.ServerMessage;
import com.github.splendor_mobile_game.websocket.communication.UserMessage;
import com.github.splendor_mobile_game.websocket.handlers.UserRequestType;
import com.github.splendor_mobile_game.websocket.handlers.reactions.BuyReservedMine;
import com.github.splendor_mobile_game.websocket.handlers.reactions.EndTurn;
import com.github.splendor_mobile_game.websocket.response.ErrorResponse;

import java.util.UUID;

public class EndTurnController<T extends GameActivity> extends Controller {

    private T gameActivity;
    private NewTurnAnnouncementMessageHandler newTurnAnnouncementMessageHandler;

    protected EndTurnController(T activity) {
        super(activity);
        this.gameActivity = activity;
        this.newTurnAnnouncementMessageHandler = new NewTurnAnnouncementMessageHandler();
    }

    public void endTurn() {

        if (Model.getRoom().getGame().getWhosTurn().equals(Model.getUserUuid()))
            this.sendRequest();
        else {
            Log.i("UserReaction", "TurnController Error: It's not your turn!");
            activity.showToast("Error: It's not your turn!");
        }
    }


    private void sendRequest() {
        EndTurn.DataDTO dataDTO = new EndTurn.DataDTO(Model.getUserUuid());
        UserMessage message = new UserMessage(UUID.randomUUID(), UserRequestType.END_TURN, dataDTO);

        CustomWebSocketClient.getInstance().send(message);
    }


    public NewTurnAnnouncementMessageHandler getNewTurnAnnouncementMessageHandler() {
        return newTurnAnnouncementMessageHandler;
    }


    public class NewTurnAnnouncementMessageHandler extends UserReaction {


        @Override
        public UserMessage react(ServerMessage serverMessage) {
            Log.i("UserReaction", "Entered NewTurnAnnouncementMessageHandler");
            EndTurn.ResponseData responseData = (EndTurn.ResponseData) ReactionUtils.getResponseData(
                    serverMessage, EndTurn.ResponseData.class
            );


            Room room = Model.getRoom();
            Game game = room.getGame();
            User user = Model.getRoom().getUserByUuid(responseData.nextUserUuid);

            game.setWhosTurn(user.getUuid());

            activity.showToast("INFO: " + user.getName() + "'s turn!");

            // TODO Update the view
            // ...
            // this.gameActivity.updateTurnIndicator()

            /*if (game.getWhosTurn().getUuid().equals(Model.getUserUuid()))
                this.gameActivity.unblockButtons();
            else
                this.gameActivity.blockButtons();*/

            // Return null if you don't want to send anything to the server
            return null;
        }


        @Override
        public UserMessage onFailure(ErrorResponse errorResponse) {
            activity.showToast("Error: " + errorResponse.data.error);
            return null;
        }

        @Override
        public UserMessage onError(ErrorResponse errorResponse) {
            activity.showToast("Error: " + errorResponse.data.error);
            return null;
        }

    }
}
