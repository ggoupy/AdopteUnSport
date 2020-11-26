package fr.univlyon1.m1if.m1if10.gr14.controller.event;

import java.io.IOException;
import javax.persistence.EntityManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import fr.univlyon1.m1if.m1if10.gr14.controller.util.ControllerHelper;
import fr.univlyon1.m1if.m1if10.gr14.dao.EventDao;
import fr.univlyon1.m1if.m1if10.gr14.dao.UserDao;
import fr.univlyon1.m1if.m1if10.gr14.dto.DtoBuilder;
import fr.univlyon1.m1if.m1if10.gr14.dto.EventDto;
import fr.univlyon1.m1if.m1if10.gr14.model.Event;
import fr.univlyon1.m1if.m1if10.gr14.model.User;
import fr.univlyon1.m1if.m1if10.gr14.route.Routes;
import fr.univlyon1.m1if.m1if10.gr14.util.StringValidator;


/**
 * Controller of the application for the join event page.
 */
@WebServlet(name = "JoinEventController", urlPatterns = {
    Routes.EVENT_JOIN_URI
})
public class JoinEventController extends HttpServlet {
    private static final String VUE_PATH = "/WEB-INF/vue/event/detailsEvent.jsp";
    private static final long serialVersionUID = 1L;


    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
    throws ServletException, IOException {
        ServletContext sc = request.getServletContext();
        EntityManager em = (EntityManager) sc.getAttribute("EntityManager");
        UserDao userDao = (UserDao) sc.getAttribute("userDAO");
        EventDao eventDao = (EventDao) sc.getAttribute("eventDAO");
        String idStr = request.getParameter("idEvent");

        //User verification
        //Should not be null as the user must be authenticated to make requests to this servlet
        User user = userDao.getUserByEmail((String) request.getAttribute("jwtUserEmail"));
        if (user == null) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de rejoindre l'évènement : vous n'êtes pas connecté.");
            return;
        }

        //Event ID verification
        Integer id = StringValidator.stringToInteger(idStr);
        if (id == null) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de retrouver l'évènement, veuillez réessayer plus tard.");
            return;
        }

        //Event verification
        Event event = eventDao.getEventById(id);
        if (event == null) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de retrouver l'évènement, veuillez réessayer plus tard.");
            return;
        }

        //Try to add the user to the event
        em.getTransaction().begin();
        int code = eventDao.addUserToEvent(user, event);
        em.getTransaction().commit();

        if (code == EventDao.CODE_EVENT_FULL) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de rejoindre l'évènement : l'évènement est complet.");
            return;
        }
        else if (code == EventDao.CODE_INCORRECT_PARAM) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de rejoindre l'évènement, veuillez réessayer plus tard.");
            return;
        }
        else if (code == EventDao.CODE_EVENT_CANCEL) {
            ControllerHelper.forwardWithError(request, response, VUE_PATH,
                "Impossible de rejoindre l'évènement : l'évènement a été annulé.");
            return;
        }
        else {
            request.setAttribute("eventdto", DtoBuilder.build(event, EventDto.class));
            ControllerHelper.addMessage(request, "L'évènement a bien été rejoint.");
            request.setAttribute("currentUserPlayer", true); //to update the display page
            request.getRequestDispatcher(VUE_PATH).forward(request, response);
        }
    }


    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
    throws ServletException, IOException {
        ControllerHelper.sendRedirect(request, response, Routes.HOME_URI);
    }
}
