package events;

import main.DBCon;
import main.Prefix;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExpDate extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        String[] command = event.getMessage().getContentRaw().split(" ");
        String date = "Неизвестно";
        long days = 0;

        if (command.length <= 1) {
//            if command options is empty
            if (command[0].equalsIgnoreCase(Prefix.PREFIX + "expdate")) {
                event.getChannel().sendTyping().queue();
                event.getChannel().sendMessage("Введите название системы").queue();
            }
        } else if (command[0].equalsIgnoreCase(Prefix.PREFIX + "expdate")) {
//            string from command options
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < command.length; i++) sb.append(command[i]).append(" ");
            String sys = sb.toString().trim();
//            expansion into 'Nagii' system is not possible
            if (sys.equalsIgnoreCase("nagii")) {
                event.getChannel().sendTyping().queue();
                event.getChannel().sendMessage("Nagii - домашняя система фракции").queue();
            } else {
//                try-with-resources
                try (Connection connection = new DBCon().getConnectionBD();
                     Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery(
                            "SELECT * FROM nagiisys WHERE systems LIKE '%" + sys + "%'");
                    while (resultSet.next()) {
                        String dateStr = resultSet.getString("date");
                        LocalDate dateSQL = LocalDate.parse(dateStr);
                        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMMM yyyy");
//                        formatted date
                        date = dateSQL.format(f);
                        long daysToExp = dateSQL.toEpochDay();
                        long daysToNow = LocalDate.now().toEpochDay();
//                        days have passed since the expansion
                        days = (daysToNow - daysToExp);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
//                if the date is not received
                if (date.equals("Неизвестно")) {
                    event.getChannel().sendTyping().queue();
                    event.getChannel().sendMessage("Проверьте название системы").queue();
                } else {
//                    text
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Экспансия в систему **" + sys.toUpperCase() + "**");
                    embed.setDescription("Была проведена " + date + "\n" +
                            "Дней со дня экспансии прошло: " + days);
                    embed.setColor(0xf56111);
//                    send text to chat
                    event.getChannel().sendTyping().queue();
                    event.getChannel().sendMessage(embed.build()).queue();
                    embed.clear();
                }
            }
        }
    }
}
