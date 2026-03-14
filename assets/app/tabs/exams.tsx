import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../../constants/theme';

export default function Exams() {
  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>📝</Text>
      <Text style={styles.title}>Exam Prep</Text>
      <Text style={styles.sub}>Coming in Session 4</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.navy,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emoji: { fontSize: 48, marginBottom: 16 },
  title: {
    fontFamily: theme.fonts.heading,
    fontSize: 24,
    color: theme.colors.gold,
  },
  sub: {
    fontFamily: theme.fonts.body,
    fontSize: 14,
    color: theme.colors.textMuted,
    marginTop: 8,
  },
});
