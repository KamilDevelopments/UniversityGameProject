package com.example.splendormobilegame.activities.GameActivity;

import android.util.Log;

import com.example.splendormobilegame.Controller;
import com.example.splendormobilegame.model.Model;
import com.example.splendormobilegame.model.Card;
import com.example.splendormobilegame.model.User;
import com.example.splendormobilegame.model.Game;
import com.example.splendormobilegame.model.ReservedCard;
import com.example.splendormobilegame.websocket.UserReaction;
import com.example.splendormobilegame.websocket.ReactionUtils;
import com.example.splendormobilegame.websocket.CustomWebSocketClient;
import com.github.splendor_mobile_game.websocket.communication.ServerMessage;
import com.github.splendor_mobile_game.websocket.communication.UserMessage;
import com.github.splendor_mobile_game.websocket.response.ErrorResponse;
import com.github.splendor_mobile_game.websocket.handlers.UserRequestType;
import com.github.splendor_mobile_game.websocket.handlers.reactions.MakeReservationFromDeck;
import java.util.UUID;


public class DeckReservingController<T extends GameActivity> extends Controller {

    private T gameActivity;
    private TurnController turnController;
    private ReservationFromDeckMessageHandler reservationFromDeckMessageHandler;

    protected DeckReservingController(T activity, TurnController turnController) {
        super(activity);
        this.gameActivity = activity;
        this.turnController = turnController;
        this.reservationFromDeckMessageHandler = new ReservationFromDeckMessageHandler();
    }

    public void reserveCard(UUID cardUuid) {
        // Maybe you want to check some things now
        // Then call the method to send request
      this.sendRequestToReserve(cardUuid);
    }

    private void sendRequestToReserve(UUID cardUuid) {
        // TODO Compose up the message to the server
        // TODO Send the message


        Card card=Model.getRoom().getGame().getCardByUuid(cardUuid);
        

        MakeReservationFromDeck.DataDTO dataDTO = new MakeReservationFromDeck.DataDTO(Model.getUserUuid(), card.getCardTier().toString());

        UserMessage userMessage = new UserMessage(UUID.randomUUID(), UserRequestType.MAKE_RESERVATION_FROM_DECK, dataDTO);
        CustomWebSocketClient.getInstance().send(userMessage);

    }

    public ReservationFromDeckMessageHandler getReservationFromDeckMessageHandler() {
        return reservationFromDeckMessageHandler;
    }

    public class ReservationFromDeckMessageHandler extends UserReaction {

        @Override
        public UserMessage react(ServerMessage serverMessage) {
            Log.i("UserReaction", "Entered ReservationFromDeckMessageHandler react method");

            
            MakeReservationFromDeck.ResponseData responseData = (MakeReservationFromDeck.ResponseData) ReactionUtils.getResponseData(serverMessage, MakeReservationFromDeck.ResponseData.class);


            User user=Model.getRoom().getUserByUuid(Model.getUserUuid());
            Card card=Model.getRoom().getGame().getCardByUuid(responseData.card.uuid);
            ReservedCard reservedCard= new ReservedCard(card,user,true );
            Model.getRoom().getGame().reserveCard(user, reservedCard);
            


            // TODO Update the view via `gameActivity` or other objects given in constructor

            // If this message pertains to me, it means I requested it, indicating that I have taken my action during my turn.
            // Therefore, I need to end my turn.


            activity.showToast("User "+user.getName()+"reserved card from deck "+card.getCardTier());

            // Perhaps it was not the best decision to require the user to manually end their turn.
            // The server should handle this automatically.
            DeckReservingController.this.turnController.endTurn();

            return null;
        }

        @Override
        public UserMessage onFailure(ErrorResponse errorResponse) {
            activity.showToast("Error while reserving from deck: " + errorResponse.data.error);
            return null;
        }

        @Override
        public UserMessage onError(ErrorResponse errorResponse) {
            activity.showToast("Error while reserving from deck: " + errorResponse.data.error);
            return null;
        }

    }

}
