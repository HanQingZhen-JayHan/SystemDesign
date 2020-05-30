package ParkingLotSystemDesign;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;


//Post paid parking lot system design.
public class ParkingLot {

    public static void main(String[] args) {
        List<SlotEntity> slotEntities = new ArrayList<>();
        slotEntities.add(new SlotEntity(SlotSize.SMALL, 1.0f, 10));
        slotEntities.add(new SlotEntity(SlotSize.MEDIUM, 1.5f, 3));
        slotEntities.add(new SlotEntity(SlotSize.LARGE, 2.0f, 2));
        IManager manager = new ParkingLotManager(slotEntities);
        ParkingLot so = new ParkingLot(manager);

        so.checkIn("BC-A0B0C0D0", VehicleType.SEDAN);
        so.checkIn("BC-A1B1C1D1", VehicleType.TRUCK);
        so.checkIn("BC-A2B2C2D2", VehicleType.SUV);
        so.checkIn("BC-A3B3C3D3", VehicleType.BUS);
        so.checkIn("BC-A4B4C4D4", VehicleType.SEDAN);
        so.checkIn("BC-A5B5C5D5", VehicleType.VAN);
        for (int i = 0; i < 10; i++) {
            try {
                // thread to sleep for 1000 milliseconds
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println(e);
            }
            so.checkout(String.format("BC-A%dB%dC%dD%d", i, i, i, i));
        }

    }

    private IManager parkingLotManager;

    public ParkingLot(IManager manager) {
        this.parkingLotManager = manager;
    }

    public ParkingLot(List<SlotEntity> slotEntities) {
        this.parkingLotManager = new ParkingLotManager(slotEntities);
    }

    public boolean checkIn(String plateNumber, VehicleType vehicleType) {
        return parkingLotManager.parkVehicle(plateNumber, vehicleType);
    }

    public double checkout(String plateNumber) {
        return parkingLotManager.unparkVehicle(plateNumber);
    }

    public int[] getVacancyCounts() {
        return parkingLotManager.getVacancyCounts();
    }

    public void updateDiscount(List<Float> discounts) {
        parkingLotManager.updateDiscount(discounts);
    }

}

// SlotManager-------------------------------------------------------------------------------------------
interface IManager {
    boolean parkVehicle(String plateNumber, VehicleType type);

    double unparkVehicle(String plateNumber);

    int[] getVacancyCounts();

    void updateDiscount(List<Float> discounts);

    List<VehicleType> getVehicleTypeListBySlotSize(SlotSize slotSize);

    TicketType getTicketTypeByPlateNumber(String plateNumber);
}

class ParkingLotManager implements IManager {

    private List<ISlot> slots;

    private int ticketIdIndex;
    private Map<String, ITicket> ticketList;

    public ParkingLotManager(List<SlotEntity> slotEntities) {
        if (slotEntities == null || slotEntities.isEmpty()) {
            slotEntities = Arrays.asList(new SlotEntity());
        }
        this.slots = new ArrayList<>(slotEntities.size());
        int total = 0;
        for (SlotEntity slotEntity : slotEntities) {
            this.slots.add(new Slot(slotEntity));
            total += slotEntity.getTotalCount();
        }
        this.ticketList = new HashMap<>(total);
    }

    private ISlot getSlotByVehicleType(VehicleType type) {
        for (ISlot slot : slots) {
            if (slot.canPark(type)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public boolean parkVehicle(String plateNumber, VehicleType type) {
        ISlot slot = getSlotByVehicleType(type);
        if (slot == null || !slot.hasVacancy()) {
            return false;
        } else {
            ticketList.put(plateNumber, new Ticket(generateTicketId(), getTicketTypeByPlateNumber(plateNumber), new Vehicle(plateNumber, type)));
            slot.parkVehicle();
            return true;
        }
    }

    @Override
    public double unparkVehicle(String plateNumber) {
        ITicket ticket = ticketList.get(plateNumber);
        if (ticket == null) {
            ticket = new Ticket(generateTicketId(), getTicketTypeByPlateNumber(plateNumber), new Vehicle(plateNumber, VehicleType.SEDAN));
        }
        ISlot slot = getSlotByVehicleType(ticket.getVehicle().getVehicleType());
        slot.unparkVehicle();
        ticketList.remove(plateNumber);

        return ticket.checkout(slot);
    }

    @Override
    public int[] getVacancyCounts() {
        int[] vacancies = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            vacancies[i] = slots.get(i).getVacancyCount();
        }
        return vacancies;
    }

    @Override
    public void updateDiscount(List<Float> discounts) {
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).setDiscount(discounts.get(i));
        }
    }

    @Override
    public List<VehicleType> getVehicleTypeListBySlotSize(SlotSize slotSize) {
        switch (slotSize) {
            case LARGE:
                return Arrays.asList(VehicleType.BUS);
            case MEDIUM:
                return Arrays.asList(VehicleType.VAN, VehicleType.TRUCK, VehicleType.WAGON);
            default:
                return Arrays.asList(VehicleType.SUV, VehicleType.SEDAN, VehicleType.COUPE);
        }
    }

    @Override
    public TicketType getTicketTypeByPlateNumber(String plateNumber) {
        //assume membership system already exist
        //currently, just return normal
        return TicketType.NON_MEMBER;
    }


    private int generateTicketId() {
        return ticketIdIndex++;
    }
}

// Slot ---------------------------------------------------------------------------------------------
enum SlotSize {
    SMALL,
    MEDIUM,
    LARGE
}

class SlotEntity {
    private int totalCount;
    private int vacancyCount;
    private SlotSize slotSize;
    private float regularPrice;
    private float discount;

    public SlotEntity() {
        this(SlotSize.SMALL, 0, 0);
    }

    public SlotEntity(SlotSize slotSize, float regularPrice, int totalCount) {
        this.slotSize = slotSize;
        this.regularPrice = regularPrice;
        this.totalCount = totalCount;
        this.vacancyCount = totalCount;
        this.discount = 0;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public void setVacancyCount(int vacancyCount) {
        this.vacancyCount = vacancyCount;
    }

    public SlotSize getSlotSize() {
        return slotSize;
    }

    public void setSlotSize(SlotSize slotSize) {
        this.slotSize = slotSize;
    }

    public float getRegularPrice() {
        return regularPrice;
    }

    public void setRegularPrice(float regularPrice) {
        this.regularPrice = regularPrice;
    }

    public float getDiscount() {
        return discount;
    }

    public void setDiscount(float discount) {
        this.discount = Math.max(0, discount);
        this.discount = Math.min(1, discount);
    }

    public int getVacancyCount() {
        return vacancyCount;
    }
}

interface ISlot {
    boolean hasVacancy();

    void parkVehicle();

    void unparkVehicle();

    double calculateFee(long startTime);

    boolean canPark(VehicleType vehicleType);

    void setDiscount(float discount);

    float getDiscount();

    void setRegularPrice(float regularPrice);

    float getRegularPrice();

    void setTotalCount(int totalCount);

    int getTotalCount();

    int getVacancyCount();

    String getTypeName();
}

//Strategy pattern
interface ISlotParkStrategy {
    List<VehicleType> getCapacityList(SlotSize slotSize);
}

class SlotParkDefaultStrategy implements ISlotParkStrategy {

    @Override
    public List<VehicleType> getCapacityList(SlotSize slotSize) {
        switch (slotSize) {
            case LARGE:
                return Arrays.asList(VehicleType.BUS);
            case MEDIUM:
                return Arrays.asList(VehicleType.VAN, VehicleType.TRUCK, VehicleType.WAGON);
            default:
                return Arrays.asList(VehicleType.SUV, VehicleType.SEDAN, VehicleType.COUPE);
        }
    }
}

class Slot implements ISlot {

    private SlotEntity slotEntity;
    private ISlotParkStrategy slotParkStrategy;
    private Set<VehicleType> capacities;

    public Slot(SlotEntity slot) {
        this(slot, null);
    }

    public Slot(SlotEntity slot, ISlotParkStrategy slotParkStrategy) {
        this.slotEntity = slot == null ? new SlotEntity() : slot;
        if (slotParkStrategy == null) {
            this.slotParkStrategy = new SlotParkDefaultStrategy();
        } else {
            this.slotParkStrategy = slotParkStrategy;
        }
        capacities = new HashSet<>(this.slotParkStrategy.getCapacityList(slot.getSlotSize()));
    }

    @Override
    public String getTypeName() {
        return slotEntity.getSlotSize().name();
    }

    @Override
    public void setTotalCount(int totalCount) {
        slotEntity.setTotalCount(totalCount);
    }

    @Override
    public int getTotalCount() {
        return slotEntity.getTotalCount();
    }

    @Override
    public int getVacancyCount() {
        return slotEntity.getVacancyCount();
    }

    @Override
    public boolean hasVacancy() {
        return slotEntity.getVacancyCount() > 0;
    }

    @Override
    public void parkVehicle() {
        if (hasVacancy()) {
            slotEntity.setVacancyCount(slotEntity.getVacancyCount() - 1);
        }
    }

    @Override
    public void unparkVehicle() {
        if (slotEntity.getVacancyCount() < slotEntity.getTotalCount()) {
            slotEntity.setVacancyCount(slotEntity.getVacancyCount() + 1);
        }
    }

    @Override
    public boolean canPark(VehicleType vehicleType) {
        return capacities.contains(vehicleType);
    }

    @Override
    public void setDiscount(float discount) {
        slotEntity.setDiscount(discount);
    }

    @Override
    public float getDiscount() {
        return slotEntity.getDiscount();
    }

    @Override
    public void setRegularPrice(float regularPrice) {
        slotEntity.setRegularPrice(regularPrice);
    }

    @Override
    public float getRegularPrice() {
        return slotEntity.getRegularPrice();
    }

    @Override
    public double calculateFee(long startTime) {
        float hours = (System.currentTimeMillis() - startTime) / 3600000.00f;
        //minimum is half an hour
        hours = Math.max(0.5f, hours);
        return hours * slotEntity.getRegularPrice() * (1 - slotEntity.getDiscount());
    }
}

// Vehicle-------------------------------------------------------------------------------------------
enum VehicleType {
    SUV,
    TRUCK,
    SEDAN,
    VAN,
    COUPE,
    WAGON,
    BUS
}

class Vehicle {

    private final String plateNumber;
    private final VehicleType vehicleType;

    protected Vehicle(String plateNumber, VehicleType vehicleType) {
        this.vehicleType = vehicleType;
        this.plateNumber = plateNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getTypeName() {
        return vehicleType.name();
    }
}

// Ticket -------------------------------------------------------------------------------------------

interface ITicket {

    Vehicle getVehicle();

    double checkout(ISlot slot);

    String printReceipt(double fee, float rate, float hours);

}

enum TicketType {
    NON_MEMBER,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

class Ticket implements ITicket {
    private int id;
    private Vehicle vehicle;
    private TicketType ticketType;
    private ISlot slot;
    private long startTime;
    private long endTime;

    public Ticket(int id, TicketType ticketType, Vehicle vehicle) {
        this.id = id;
        this.vehicle = vehicle;
        this.ticketType = ticketType;
        this.startTime = System.currentTimeMillis();
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public double checkout(ISlot slot) {
        if (ticketType == null) {
            ticketType = TicketType.NON_MEMBER;
        }
        if (ticketType == TicketType.NON_MEMBER) {
            if (slot == null) {
                slot = new Slot(new SlotEntity());
            }
            this.slot = slot;
            endTime = System.currentTimeMillis();
            float hours = (endTime - startTime) / 3600000.00f;
            float rate = this.slot.getRegularPrice();
            //minimum is half an hour
            hours = Math.max(0.5f, hours);
            double fee = hours * rate * (1 - this.slot.getDiscount());
            System.out.println(printReceipt(fee, rate, hours));

            return fee;
        } else {
            //assume membership system will handle these scenarios
            return 0;
        }

    }

    @Override
    public String printReceipt(double fee, float rate, float hours) {
        StringBuilder sb = new StringBuilder();
        sb.append("System Design Parking Lot Receipt")
                .append("\n")
                .append("\n")
                .append(convertTime(System.currentTimeMillis()))
                .append("\n")
                .append("Ticket Id:      " + id)
                .append("\n")
                .append("Plate Number:   " + vehicle.getPlateNumber())
                .append("\n")
                .append("Vehicle Type:   " + vehicle.getTypeName())
                .append("\n")
                .append("Park Lot Type:  " + slot.getTypeName())
                .append("\n")
                .append("Time:           " + convertTime(startTime))
                .append(" - ")
                .append(convertTime(endTime))
                .append("\n")
                .append("Duration:       " + hours + " hours")
                .append("\n")
                .append("Rate:           " + rate + " $CAD/Hour")
                .append("\n")
                .append("---------------------------------------------")
                .append("\n")
                .append("Total Fee(include Tax): " + fee + " $CAD")
                .append("\n")
                .append("\n")
                .append("\n");
        return sb.toString();
    }

    public String convertTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
}






