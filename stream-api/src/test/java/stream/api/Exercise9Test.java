package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.entity.Item;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringJoiner> supplier = () -> new StringJoiner(",");
        BiConsumer<StringJoiner, String> accumulator = StringJoiner::add;
        BinaryOperator<StringJoiner> combiner = StringJoiner::merge;
        Function<StringJoiner, String> finisher = StringJoiner::toString;

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    private Consumer<Item> getItemConsumer(Map<String, Set<String>> m, String customerName) {
        return item -> m.compute(item.getName(), (k, v) -> v == null ? new HashSet<>(Collections.singletonList(customerName)) : addToSet(v, customerName));
    }

    private <T> Set<T> addToSet(Set<T> set, T value) {
        set.add(value);
        return set;
    }

    private <T, U> Map<T, U> mergeMaps(Map<T, U> map1, Map<T, U> map2, BiFunction<? super U, ? super U, ? extends U> biFunction) {
        map2.forEach((k, v) -> map1.merge(k, v, biFunction));
        return map1;
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String, Set<String>>> supplier = HashMap::new;
        BiConsumer<Map<String, Set<String>>, Customer> accumulator = (m, c) -> c.getWantToBuy().forEach(getItemConsumer(m, c.getName()));
        BinaryOperator<Map<String, Set<String>>> combiner = (m1, m2) -> mergeMaps(m1, m2, (s1, s2) -> {s1.addAll(s2); return s1;});
        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */
        Supplier<SortedSet<Integer>> supplier = TreeSet::new;
        BiConsumer<SortedSet<Integer>, String> accumulator = (set, s) -> {
            List<Integer> range = Arrays.stream(s.split("-")).map(Integer::parseInt).collect(Collectors.toList());
            if (range.size() == 1) {
                set.add(range.get(0));
            } else {
                IntStream.range(range.get(0), range.get(1)+1).forEach(set::add);
            }
        };
        BinaryOperator<SortedSet<Integer>> combiner = null;
        Function<SortedSet<Integer>, String> finisher = s -> {
            Integer maxInteger = s.last();
            return IntStream.range(1, maxInteger+1).mapToObj(i -> s.contains(i) ? "1" : "0").collect(Collectors.joining());
        };
        Collector<String, ?, String> toBitString = new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());

        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
