import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { theme } from '../../constants/theme';

const cards = [
  { title: 'Case Way', subtitle: 'Get quick case summaries', emoji: '??' },
  { title: 'Exam Quicknotes', subtitle: 'Daily MCQ revision', emoji: '??' },
  { title: 'News Feed', subtitle: 'Law updates & alerts', emoji: '??' },
];

export default function Home() {
  return (
    <View style={styles.container}>
      <View style={styles.top}>
        <Text style={styles.hello}>Welcome back, Advocate ??</Text>
        <Text style={styles.subtitle}>Study smarter. Master law faster.</Text>
      </View>
      <View style={styles.cards}>
        {cards.map((card) => (
          <TouchableOpacity key={card.title} style={styles.card}>
            <Text style={styles.cardEmoji}>{card.emoji}</Text>
            <Text style={styles.cardTitle}>{card.title}</Text>
            <Text style={styles.cardSub}>{card.subtitle}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <View style={styles.footer}>
        <Text style={styles.footerTitle}>Daily Legal Sprint</Text>
        <Text style={styles.footerText}>Complete 3 quick practice questions before 9 PM.</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.navy,
    padding: theme.spacing.lg,
  },
  top: {
    marginTop: theme.spacing.lg,
    marginBottom: theme.spacing.lg,
  },
  hello: {
    fontFamily: theme.fonts.heading,
    fontSize: 28,
    color: theme.colors.gold,
  },
  subtitle: {
    marginTop: 6,
    fontFamily: theme.fonts.body,
    color: theme.colors.textSecondary,
    fontSize: 15,
  },
  cards: {
    marginTop: theme.spacing.lg,
    gap: 10,
  },
  card: {
    backgroundColor: theme.colors.cardBg,
    borderColor: theme.colors.cardBorder,
    borderWidth: 1,
    borderRadius: theme.radius.lg,
    padding: theme.spacing.md,
  },
  cardEmoji: {
    fontSize: 24,
    marginBottom: 8,
  },
  cardTitle: {
    fontFamily: theme.fonts.heading,
    fontSize: 18,
    color: theme.colors.gold,
  },
  cardSub: {
    marginTop: 4,
    fontFamily: theme.fonts.body,
    color: theme.colors.textSecondary,
  },
  footer: {
    marginTop: 'auto',
    borderTopWidth: 1,
    borderTopColor: theme.colors.cardBorder,
    paddingTop: theme.spacing.md,
  },
  footerTitle: {
    fontFamily: theme.fonts.heading,
    color: theme.colors.gold,
    fontSize: 18,
  },
  footerText: {
    marginTop: 4,
    fontFamily: theme.fonts.body,
    color: theme.colors.textSecondary,
  },
});
